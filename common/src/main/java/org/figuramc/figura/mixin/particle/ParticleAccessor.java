package org.figuramc.figura.mixin.particle;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Particle.class)
public interface ParticleAccessor {

    @Accessor
    void setGravity(float gravity);

    @Accessor
    float getGravity();

    @Accessor
    void setHasPhysics(boolean physics);

    @Accessor
    boolean getHasPhysics();

    @Accessor
    void setXo(double xo);

    @Accessor
    void setYo(double yo);

    @Accessor
    void setZo(double zo);

    @Accessor("x")
    double getX();

    @Accessor("y")
    double getY();

    @Accessor("z")
    double getZ();

    @Accessor
    float getBbWidth();

    @Accessor
    double getXd();

    @Accessor
    double getYd();

    @Accessor
    double getZd();
}
