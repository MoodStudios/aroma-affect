// CURIOS-DISABLED (no 26.2 release) — uncomment this whole file when Curios publishes for 26.2.
//package com.ovrtechnology.neoforge.client.accessory;
//
//import com.ovrtechnology.nose.NoseItem;
//import com.ovrtechnology.nose.NoseRegistry;
//import net.minecraft.core.Holder;
//import net.minecraft.world.item.Item;
//import top.theillusivec4.curios.api.client.ICurioRenderer;
//
//public final class CuriosClientIntegration {
//
//    private CuriosClientIntegration() {}
//
//    public static void init() {
//        for (Holder<Item> holder : NoseRegistry.getAllNoses()) {
//            if (holder.isBound() && holder.value() instanceof NoseItem noseItem) {
//                ICurioRenderer.register(noseItem, NoseCurioRenderer::new);
//            }
//        }
//        for (Holder<Item> holder : NoseRegistry.getLegacyItems()) {
//            if (holder.isBound() && holder.value() instanceof NoseItem noseItem) {
//                ICurioRenderer.register(noseItem, NoseCurioRenderer::new);
//            }
//        }
//    }
//}
