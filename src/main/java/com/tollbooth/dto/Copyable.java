package com.tollbooth.dto;

public interface Copyable<T> {

  /**
   * Return a deep copy of this object.
   *
   * @return the copy
   */
  T deepCopy();
}
