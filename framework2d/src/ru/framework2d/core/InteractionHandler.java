package ru.framework2d.core;

public interface InteractionHandler {
	
	public boolean isInteractionPossible(Component firstComponent, Component secondComponent);
	
	public boolean isInteractionHappened(Component firstComponent, Component secondComponent);
	
	public boolean toHandleInteraction(Component firstComponent, Component secondComponent);
}
