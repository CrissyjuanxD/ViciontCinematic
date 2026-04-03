package com.vctcinematics.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record VctCinePayload(byte action, String name, int index, double x, double y, double z, float yaw, float pitch, String transition, long timeMs, String type, boolean hasFade, int loopCount) implements CustomPayload {
    public static final Id<VctCinePayload> ID = new Id<>(Identifier.of("viciontcinematics", "main"));

    public static final PacketCodec<RegistryByteBuf, VctCinePayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeByte(value.action);
                buf.writeString(value.name != null ? value.name : "none");
                buf.writeVarInt(value.index);
                buf.writeDouble(value.x);
                buf.writeDouble(value.y);
                buf.writeDouble(value.z);
                buf.writeFloat(value.yaw);
                buf.writeFloat(value.pitch);
                buf.writeString(value.transition != null ? value.transition : "NORMAL");
                buf.writeVarLong(value.timeMs);
                buf.writeString(value.type != null ? value.type : "SMOOTH");
                buf.writeBoolean(value.hasFade);
                buf.writeVarInt(value.loopCount);
            },
            buf -> new VctCinePayload(
                    buf.readByte(),
                    buf.readString(),
                    buf.readVarInt(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readString(),
                    buf.readVarLong(),
                    buf.readString(),
                    buf.readBoolean(),
                    buf.readVarInt()
            )
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}