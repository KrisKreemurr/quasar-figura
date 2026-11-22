package org.figuramc.figura.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.gui.screens.AvatarScreen;
import org.figuramc.figura.model.rendering.EntityRenderMode;
import org.figuramc.figura.utils.FiguraIdentifier;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.ui.UIHelper;
import org.joml.Matrix3x2fStack;
import org.joml.Vector3f;

public class EntityPreview extends AbstractContainerElement {

    public static final Identifier UNKNOWN = new FiguraIdentifier("textures/gui/unknown_entity.png");
    public static final Identifier OVERLAY = new FiguraIdentifier("textures/gui/entity_overlay.png");

    // properties
    private LivingEntity entity;
    private final float pitch, yaw, scale;
    private SwitchButton button;

    // transformation data

    // rot
    private boolean isRotating = false;
    private float anchorX = 0f, anchorY = 0f;
    private float anchorAngleX = 0f, anchorAngleY = 0f;
    private float angleX, angleY;

    // scale
    private float scaledValue = 0f, scaledPrecise = 0f;
    private static final float SCALE_FACTOR = 1.1F;

    // pos
    private boolean isDragging = false;
    private int modelX, modelY;
    private float dragDeltaX, dragDeltaY;
    private float dragAnchorX, dragAnchorY;

    public EntityPreview(int x, int y, int width, int height, float scale, float pitch, float yaw, LivingEntity entity, Screen parentScreen) {
        super(x, y, width, height);

        this.scale = scale;
        this.pitch = pitch;
        this.yaw = yaw;
        this.entity = entity;

        modelX = width / 2;
        modelY = height / 2;
        angleX = pitch;
        angleY = yaw;

        // button
        children.add(button = new SwitchButton(
                x + 4, y + 4, 16, 16,
                0, 0, 16,
                new FiguraIdentifier("textures/gui/expand.png"),
                48, 32,
                FiguraText.of("gui.expand"),
                bx -> {
                    if (button.isToggled()) {
                        Minecraft.getInstance().gui.setScreen(new AvatarScreen(scale, pitch, yaw, this.entity, parentScreen));
                    } else {
                        Minecraft.getInstance().gui.setScreen(parentScreen);
                    }
                }));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float delta) {
        if (!this.isVisible())
            return;

        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();

        if (!button.isToggled()) {
            // border
            UIHelper.blitSliced(gui, x, y, width, height, UIHelper.OUTLINE_FILL);
            // overlay
            UIHelper.blit(gui, x + 1, y + 1, width - 2, height, OVERLAY);
        }

        // Quasar MC (26.3): poll the mouse buttons directly so rotate/move works even when the
        // screen's click/drag events don't reach this widget.
        figura$pollMouse(mouseX, mouseY);

        // scissors
        gui.enableScissor(x + 1, y + 1, x + width - 1, y + height - 1);

        // render entity
        Matrix3x2fStack pose = gui.pose();
        if (entity != null) {
            pose.pushMatrix();
            scaledValue = Mth.lerp((float) (1f - Math.pow(0.5f, delta)), scaledValue, scaledPrecise);
            UIHelper.drawEntity(x + modelX, y + modelY, scale + scaledValue, angleX, angleY, entity, gui, new Vector3f(), EntityRenderMode.FIGURA_GUI, x, y, x+width, y+height);
            pose.popMatrix();
        } else {
            // draw
            int s = Math.min(width, height) * 2 / 3;
            UIHelper.enableBlend();
            gui.blit(RenderPipelines.GUI_TEXTURED, UNKNOWN, x + (width - s) / 2, y + (height - s) / 2, 0f, 64 * ((int) (FiguraMod.ticks / 3f) % 8), s, s,64, 64, 64, 512);
        }

        gui.disableScissor();

        super.extractRenderState(gui, mouseX, mouseY, delta);
    }

    // Quasar MC (26.3): rotate / move / reset are read straight from GLFW in figura$pollMouse.
    // The click events only claim the click so the screen keeps this widget focused.
    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
        if (!this.isVisible() || !this.isMouseOver(mouseButtonEvent.x(), mouseButtonEvent.y()))
            return false;
        return super.mouseClicked(mouseButtonEvent, bl) || true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        return super.mouseReleased(mouseButtonEvent) || isRotating || isDragging;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double deltaX, double deltaY) {
        return isRotating || isDragging || super.mouseDragged(mouseButtonEvent, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount, double d) {
        if (!this.isVisible())
            return false;

        if (super.mouseScrolled(mouseX, mouseY, amount, d))
            return true;

        // scroll - scale

        // set scale direction
        float scaleDir = ((amount+d) > 0) ? SCALE_FACTOR : 1 / SCALE_FACTOR;

        // determine scale
        scaledPrecise = ((scale + scaledPrecise) * scaleDir) - scale;

        return true;
    }

    // -- Quasar MC (26.3): polled input -- //
    private boolean wasLeft, wasRight, wasMiddle;

    private void figura$pollMouse(int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() == null)
            return;
        // MouseHandler only tracks buttons while no screen is open, so ask GLFW directly.
        long window = mc.getWindow().handle();
        boolean left = org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        boolean right = org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        boolean middle = org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        boolean over = this.isMouseOver(mouseX, mouseY) && !button.isMouseOver(mouseX, mouseY);

        // press edges inside the preview
        if (left && !wasLeft && over && !isRotating) {
            anchorX = mouseX;
            anchorY = mouseY;
            anchorAngleX = angleX;
            anchorAngleY = angleY;
            isRotating = true;
        }
        if (right && !wasRight && over && !isDragging) {
            dragDeltaX = mouseX;
            dragDeltaY = mouseY;
            dragAnchorX = modelX;
            dragAnchorY = modelY;
            isDragging = true;
        }
        if (middle && !wasMiddle && over) {
            isRotating = false;
            isDragging = false;
            angleX = pitch;
            angleY = yaw;
            scaledValue = 0f;
            scaledPrecise = 0f;
            modelX = getWidth() / 2;
            modelY = getHeight() / 2;
        }

        // releases
        if (!left) isRotating = false;
        if (!right) isDragging = false;

        // apply movement
        if (isRotating) {
            double factor = 3d / mc.getWindow().getGuiScale();
            angleX = (float) (anchorAngleX + (anchorY - mouseY) / factor);
            angleY = (float) (anchorAngleY - (anchorX - mouseX) / factor);
            if (angleX >= 360 || angleX <= -360) { anchorY = mouseY; anchorAngleX = 0; angleX = 0; }
            if (angleY >= 360 || angleY <= -360) { anchorX = mouseX; anchorAngleY = 0; angleY = 0; }
        } else if (isDragging) {
            modelX = (int) (dragAnchorX + (mouseX - dragDeltaX));
            modelY = (int) (dragAnchorY + (mouseY - dragDeltaY));
        }

        wasLeft = left;
        wasRight = right;
        wasMiddle = middle;
    }

    public void setEntity(LivingEntity entity) {
        this.entity = entity;
    }

    public void setToggled(boolean toggled) {
        this.button.setToggled(toggled);
        this.button.setTooltip(toggled ? FiguraText.of("gui.minimise") : FiguraText.of("gui.expand"));
    }
}
