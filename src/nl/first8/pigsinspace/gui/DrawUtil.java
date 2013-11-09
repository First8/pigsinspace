package nl.first8.pigsinspace.gui;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glVertex2f;

public class DrawUtil {

    public static void colored_rect(int x, int y, int width, int height, float r, float g, float b) {
        glPushMatrix();
        glColor3f(r,g,b);
        // draw quad
        glBegin(GL_QUADS);
            glVertex2i(x,y);
            glVertex2i(x+width,y);
            glVertex2i(x+width,y+height);
            glVertex2i(x,y+height);
        glEnd();

        glPopMatrix();
    }

}
