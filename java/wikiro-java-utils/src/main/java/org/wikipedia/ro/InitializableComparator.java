package org.wikipedia.ro;

import java.util.Comparator;

public interface InitializableComparator<T> extends Comparator<T> {
    InitializableComparator<T> init(String prefix);
}
