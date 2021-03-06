/******************************************************************************
 * Copyright (c) 2015, 2016 itemis AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Nyßen (itemis AG) - initial API and implementation
 *     
 *******************************************************************************/
package org.eclipse.gef4.common.beans.value;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.gef4.common.collections.ObservableMultiset;

import com.google.common.collect.Multiset;

import javafx.beans.value.ObservableListValue;
import javafx.beans.value.ObservableMapValue;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableSetValue;

/**
 * An observable reference to an {@link ObservableMultiset}.
 * <p>
 * This interface provides identical functionality for {@link Multiset} as
 * {@link ObservableMapValue} for {@link Map}, {@link ObservableSetValue} for
 * {@link Set}, or {@link ObservableListValue} for {@link List}.
 * 
 * @author anyssen
 *
 * @param <E>
 *            The element type of the {@link ObservableMultiset}.
 */
public interface ObservableMultisetValue<E> extends
		ObservableObjectValue<ObservableMultiset<E>>, ObservableMultiset<E> {

}
