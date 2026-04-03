package com.vctcinematics;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.vctcinematics.core.Cinematic;
import com.vctcinematics.core.CinematicManager;
import com.vctcinematics.core.Interpolator;
import com.vctcinematics.core.Keyframe;
import com.vctcinematics.core.TransitionType;
import com.vctcinematics.network.VctCinePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.Arrays;

public class ViciontCinematicClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(CinematicManager::tick);

		PayloadTypeRegistry.playS2C().register(VctCinePayload.ID, VctCinePayload.CODEC);

		ClientPlayNetworking.registerGlobalReceiver(VctCinePayload.ID, (payload, context) -> {
			context.client().execute(() -> {
				String name = payload.name();
				switch (payload.action()) {
					case 0: // PLAY
						Interpolator.Type playType = payload.type().equals("LINEAR") ? Interpolator.Type.LINEAR : Interpolator.Type.SMOOTH;
						CinematicManager.play(name, playType, payload.hasFade(), payload.loopCount());
						break;
					case 1: // STOP
						CinematicManager.stop(name);
						break;
					case 2: // CLEAR ALL KEYFRAMES OR CREATE
						CinematicManager.cinematics.put(name, new Cinematic(name));
						break;
					case 3: // ADD KEYFRAME
						Cinematic cine = CinematicManager.cinematics.get(name);
						if (cine != null) {
							TransitionType tType = payload.transition().equals("CUT") ? TransitionType.CUT : TransitionType.NORMAL;
							cine.addOrUpdateKeyframe(new Keyframe(payload.index(), payload.x(), payload.y(), payload.z(), payload.yaw(), payload.pitch(), tType, payload.timeMs()));
						}
						break;
					case 4: // TOGGLE DEBUG DESDE EL PLUGIN
						toggleDebugLogic(name, payload.type());
						break;
				}
			});
		});

		SuggestionProvider<FabricClientCommandSource> CINEMATICS_SUGGESTIONS = (context, builder) -> CommandSource.suggestMatching(CinematicManager.cinematics.keySet(), builder);
		SuggestionProvider<FabricClientCommandSource> KF_TYPE_SUGGESTIONS = (context, builder) -> CommandSource.suggestMatching(Arrays.asList("normal", "cut"), builder);
		SuggestionProvider<FabricClientCommandSource> PLAY_TYPE_SUGGESTIONS = (context, builder) -> CommandSource.suggestMatching(Arrays.asList("smooth", "linear"), builder);
		SuggestionProvider<FabricClientCommandSource> FADE_SUGGESTIONS = (context, builder) -> CommandSource.suggestMatching(Arrays.asList("fadeon", "fadeoff"), builder);
		SuggestionProvider<FabricClientCommandSource> DEBUG_MODES = (context, builder) -> CommandSource.suggestMatching(Arrays.asList("normal", "smooth", "linear"), builder);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

			dispatcher.register(ClientCommandManager.literal("vctcinema")
					.then(ClientCommandManager.literal("crear")
							.then(ClientCommandManager.argument("nombre", StringArgumentType.word())
									.executes(context -> {
										String name = StringArgumentType.getString(context, "nombre");
										CinematicManager.cinematics.put(name, new Cinematic(name));
										context.getSource().sendFeedback(Text.literal("§aCinemática '" + name + "' creada."));
										return 1;
									})
							)
					)
					.then(ClientCommandManager.literal("list")
							.then(ClientCommandManager.argument("nombre", StringArgumentType.word()).suggests(CINEMATICS_SUGGESTIONS)
									.executes(context -> {
										String name = StringArgumentType.getString(context, "nombre");
										Cinematic cine = CinematicManager.cinematics.get(name);
										if (cine == null) {
											context.getSource().sendFeedback(Text.literal("§cLa cinemática no existe."));
											return 1;
										}

										float totalSec = cine.getTotalDuration() / 1000.0f;
										context.getSource().sendFeedback(Text.literal("§6=== Cinemática: §e" + name + " §6==="));
										context.getSource().sendFeedback(Text.literal("§7Duración total: §f" + cine.getTotalDuration() + "ms (" + String.format("%.1f", totalSec) + "s)"));

										for (Keyframe kf : cine.keyframes) {
											float kfSec = kf.timeMs / 1000.0f;
											context.getSource().sendFeedback(Text.literal("§8- §aKF " + kf.index + " §7| Tipo: §b" + kf.transition.name() + " §7| Tiempo: §e" + kf.timeMs + "ms (" + String.format("%.1f", kfSec) + "s)"));
										}
										return 1;
									})
							)
					)
					.then(ClientCommandManager.literal("keyframe")
							.then(ClientCommandManager.argument("nombre", StringArgumentType.word()).suggests(CINEMATICS_SUGGESTIONS)
									.then(ClientCommandManager.literal("add")
											.then(ClientCommandManager.argument("numero", IntegerArgumentType.integer())
													.then(ClientCommandManager.argument("tipo", StringArgumentType.word()).suggests(KF_TYPE_SUGGESTIONS)
															.then(ClientCommandManager.argument("tiempo", LongArgumentType.longArg())
																	.executes(context -> {
																		String name = StringArgumentType.getString(context, "nombre");
																		int num = IntegerArgumentType.getInteger(context, "numero");
																		String tipo = StringArgumentType.getString(context, "tipo").toUpperCase();
																		long time = LongArgumentType.getLong(context, "tiempo");

																		Cinematic cine = CinematicManager.cinematics.get(name);
																		if (cine == null) {
																			context.getSource().sendFeedback(Text.literal("§cLa cinemática no existe."));
																			return 1;
																		}

																		PlayerEntity player = MinecraftClient.getInstance().player;
																		TransitionType tType = tipo.equals("CUT") ? TransitionType.CUT : TransitionType.NORMAL;

																		Keyframe kf = new Keyframe(num, player.getX(), player.getY() + player.getStandingEyeHeight(), player.getZ(), player.getYaw(), player.getPitch(), tType, time);
																		cine.addOrUpdateKeyframe(kf);

																		context.getSource().sendFeedback(Text.literal("§aKeyframe " + num + " añadido a '" + name + "'."));
																		return 1;
																	})
															)
													)
											)
									)
									.then(ClientCommandManager.literal("edit")
											.then(ClientCommandManager.argument("numero", IntegerArgumentType.integer())
													.then(ClientCommandManager.argument("tipo", StringArgumentType.word()).suggests(KF_TYPE_SUGGESTIONS)
															.then(ClientCommandManager.argument("tiempo", LongArgumentType.longArg())
																	.executes(context -> {
																		String name = StringArgumentType.getString(context, "nombre");
																		int num = IntegerArgumentType.getInteger(context, "numero");
																		String tipo = StringArgumentType.getString(context, "tipo").toUpperCase();
																		long time = LongArgumentType.getLong(context, "tiempo");

																		Cinematic cine = CinematicManager.cinematics.get(name);
																		if (cine != null) {
																			TransitionType tType = tipo.equals("CUT") ? TransitionType.CUT : TransitionType.NORMAL;
																			if (cine.editKeyframe(num, tType, time)) {
																				context.getSource().sendFeedback(Text.literal("§aKeyframe " + num + " editado exitosamente."));
																			} else {
																				context.getSource().sendFeedback(Text.literal("§cEl keyframe " + num + " no existe."));
																			}
																		}
																		return 1;
																	})
															)
													)
											)
									)
									.then(ClientCommandManager.literal("remove")
											.then(ClientCommandManager.argument("numero", IntegerArgumentType.integer())
													.executes(context -> {
														String name = StringArgumentType.getString(context, "nombre");
														int num = IntegerArgumentType.getInteger(context, "numero");
														Cinematic cine = CinematicManager.cinematics.get(name);

														if (cine != null && cine.removeKeyframe(num)) {
															context.getSource().sendFeedback(Text.literal("§aKeyframe " + num + " eliminado."));
														} else {
															context.getSource().sendFeedback(Text.literal("§cError al eliminar."));
														}
														return 1;
													})
											)
									)
							)
					)

					// COMANDO PLAY CON OPCIÓN [LOOP]
					.then(ClientCommandManager.literal("play")
							.then(ClientCommandManager.argument("nombre", StringArgumentType.word()).suggests(CINEMATICS_SUGGESTIONS)
									.then(ClientCommandManager.argument("tipo", StringArgumentType.word()).suggests(PLAY_TYPE_SUGGESTIONS)
											.then(ClientCommandManager.argument("fade", StringArgumentType.word()).suggests(FADE_SUGGESTIONS)
													// Variante 1: Sin Loop (por defecto 1)
													.executes(context -> executeLocalPlay(context, 1))
													// Variante 2: Con loop
													.then(ClientCommandManager.argument("loop", IntegerArgumentType.integer())
															.executes(context -> executeLocalPlay(context, IntegerArgumentType.getInteger(context, "loop")))
													)
											)
									)
							)
					)

					.then(ClientCommandManager.literal("stop")
							.then(ClientCommandManager.argument("nombre", StringArgumentType.word()).suggests(CINEMATICS_SUGGESTIONS)
									.executes(context -> {
										String name = StringArgumentType.getString(context, "nombre");
										CinematicManager.stop(name);
										context.getSource().sendFeedback(Text.literal("§cSe ordenó detener la cinemática '" + name + "' (si estaba activa)."));
										return 1;
									})
							)
					)

					.then(ClientCommandManager.literal("debug")
							.then(ClientCommandManager.argument("nombre", StringArgumentType.word()).suggests(CINEMATICS_SUGGESTIONS)
									.executes(context -> toggleDebugLocal(context, StringArgumentType.getString(context, "nombre"), "normal"))
									.then(ClientCommandManager.argument("modo", StringArgumentType.word()).suggests(DEBUG_MODES)
											.executes(context -> toggleDebugLocal(context, StringArgumentType.getString(context, "nombre"), StringArgumentType.getString(context, "modo")))
									)
							)
					)
			);
		});
	}

	private static int executeLocalPlay(CommandContext<FabricClientCommandSource> context, int loop) {
		String name = StringArgumentType.getString(context, "nombre");
		String tipo = StringArgumentType.getString(context, "tipo").toUpperCase();
		boolean hasFade = StringArgumentType.getString(context, "fade").equalsIgnoreCase("fadeon");

		Interpolator.Type iType = Interpolator.Type.SMOOTH;
		if (tipo.equals("LINEAR")) iType = Interpolator.Type.LINEAR;

		CinematicManager.play(name, iType, hasFade, loop);
		context.getSource().sendFeedback(Text.literal("§6Reproduciendo '" + name + "' (Bucle: " + loop + ")..."));
		return 1;
	}

	public static void toggleDebugLogic(String name, String modeStr) {
		if (name.equalsIgnoreCase("off")) {
			CinematicManager.debugCinematic = null;
			CinematicManager.debugLineType = null;
			return;
		}

		Cinematic cine = CinematicManager.cinematics.get(name);
		if (cine == null) return;

		Interpolator.Type desiredLineType = null;
		if (modeStr.equalsIgnoreCase("smooth")) desiredLineType = Interpolator.Type.SMOOTH;
		if (modeStr.equalsIgnoreCase("linear")) desiredLineType = Interpolator.Type.LINEAR;

		if (CinematicManager.debugCinematic == cine && CinematicManager.debugLineType == desiredLineType) {
			CinematicManager.debugCinematic = null;
			CinematicManager.debugLineType = null;
		} else {
			CinematicManager.debugCinematic = cine;
			CinematicManager.debugLineType = desiredLineType;
		}
	}

	private static int toggleDebugLocal(CommandContext<FabricClientCommandSource> context, String name, String modeStr) {
		if (name.equalsIgnoreCase("off")) {
			toggleDebugLogic("off", modeStr);
			context.getSource().sendFeedback(Text.literal("§cDebug desactivado."));
			return 1;
		}

		Cinematic cine = CinematicManager.cinematics.get(name);
		if (cine == null) {
			context.getSource().sendFeedback(Text.literal("§cLa cinemática no existe."));
			return 1;
		}

		toggleDebugLogic(name, modeStr);

		if (CinematicManager.debugCinematic == null) {
			context.getSource().sendFeedback(Text.literal("§cDebug desactivado."));
		} else {
			context.getSource().sendFeedback(Text.literal("§aDebug activado para '" + name + "' en modo " + modeStr.toUpperCase() + "."));
		}
		return 1;
	}
}