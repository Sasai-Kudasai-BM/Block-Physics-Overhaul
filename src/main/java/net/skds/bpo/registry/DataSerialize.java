package net.skds.bpo.registry;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.IDataSerializer;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DataSerializerEntry;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.skds.bpo.BPO;

public class DataSerialize {

    public static final DeferredRegister<DataSerializerEntry> SERIALIZERS = DeferredRegister.create(ForgeRegistries.DATA_SERIALIZERS, BPO.MOD_ID);

	public static final IDataSerializer<Vector3d> VS = new IDataSerializer<Vector3d>() {
		@Override
		public void write(PacketBuffer buf, Vector3d vec) {
			buf.writeDouble(vec.x);
			buf.writeDouble(vec.y);
			buf.writeDouble(vec.z);
		}
		@Override
		public Vector3d read(PacketBuffer buf) {
			return new Vector3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
		}
		@Override
		public Vector3d copyValue(Vector3d vec) {
			return vec;
		}		
	};

	public static final RegistryObject<DataSerializerEntry> VEC_SERIALIZER = SERIALIZERS.register("vec3d_serializer", () -> new DataSerializerEntry(VS));

	public static void register() {		
		SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
	}
}