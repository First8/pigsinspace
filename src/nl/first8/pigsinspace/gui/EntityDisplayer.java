package nl.first8.pigsinspace.gui;

/**
 * A class that can draw specific entities.
 */
public abstract class EntityDisplayer<T> {
    /**
     * Draws the given entity
     * 
     * @param entity
     *            the entity to draw
     *            @param mode the draw mode (coloring tiles or entities)
     *            @param color the color to draw in
     */
    public abstract void draw(T entity, DrawMode mode, float[] color);
}
