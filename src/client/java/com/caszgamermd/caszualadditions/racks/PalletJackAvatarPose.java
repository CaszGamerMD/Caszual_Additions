package com.caszgamermd.caszualadditions.racks;

/** Per-avatar render-state data: safe when entity rendering is threaded. */
public interface PalletJackAvatarPose {
    void caszual_additions$setPullingJack(boolean pulling,float walking);
    boolean caszual_additions$isPullingJack();
    float caszual_additions$walkingJack();
}
