package com.tollbooth.dto;

/** Interface for entities that have an identifier. */
public interface Identifiable<T> {
  /** Returns the unique identifier for this entity. */
  T getId();
}
