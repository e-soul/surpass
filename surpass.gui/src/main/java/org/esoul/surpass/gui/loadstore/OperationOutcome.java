package org.esoul.surpass.gui.loadstore;

/**
 * Display the outcome of an operation.
 * 
 * @author mgp
 *
 * @param <T>
 */
@FunctionalInterface
public interface OperationOutcome<T> {

	void display(T context);
}
