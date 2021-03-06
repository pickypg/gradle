/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.tasks;

import groovy.lang.Closure;
import org.gradle.api.*;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.model.internal.core.ModelNode;
import org.gradle.model.internal.core.MutableModelNode;
import org.gradle.model.internal.type.ModelType;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RealizableTaskCollection<T extends Task> implements TaskCollection<T>, Iterable<T> {
    private final TaskCollection<T> delegate;
    private final Class<T> type;
    private final ProjectInternal project;
    private final AtomicBoolean realized = new AtomicBoolean(false);


    public RealizableTaskCollection(Class<T> type, TaskCollection<T> delegate, ProjectInternal project) {
        this.delegate = delegate;
        this.type = type;
        this.project = project;
    }

    public void realizeRuleTaskTypes() {
        if (realized.compareAndSet(false, true)) {
            ModelNode modelNode = project.getModelRegistry().atStateOrLater(TaskContainerInternal.MODEL_PATH, ModelNode.State.SelfClosed);
            MutableModelNode taskContainerNode = (MutableModelNode) modelNode;
            Iterable<? extends MutableModelNode> links = taskContainerNode.getLinks(ModelType.of(type));
            for (MutableModelNode node : links) {
                project.getModelRegistry().realizeNode(node.getPath());
            }
        }
    }

    @Override
    public TaskCollection<T> matching(Spec<? super T> spec) {
        return delegate.matching(spec);
    }

    @Override
    public TaskCollection<T> matching(Closure closure) {
        return delegate.matching(closure);
    }

    @Override
    public T getByName(String name, Closure configureClosure) throws UnknownTaskException {
        return delegate.getByName(name, configureClosure);
    }

    @Override
    public T getByName(String name) throws UnknownTaskException {
        return delegate.getByName(name);
    }

    @Override
    public <S extends T> TaskCollection<S> withType(Class<S> type) {
        return delegate.withType(type);
    }

    @Override
    public Action<? super T> whenTaskAdded(Action<? super T> action) {
        return delegate.whenTaskAdded(action);
    }

    @Override
    public void whenTaskAdded(Closure closure) {
        delegate.whenTaskAdded(closure);
    }

    @Override
    public T getAt(String name) throws UnknownTaskException {
        return delegate.getAt(name);
    }

    @Override
    public Set<T> findAll(Closure spec) {
        return delegate.findAll(spec);
    }

    @Override
    public boolean add(T e) {
        return delegate.add(e);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return delegate.addAll(c);
    }

    @Override
    public Namer<T> getNamer() {
        return delegate.getNamer();
    }

    @Override
    public SortedMap<String, T> getAsMap() {
        return delegate.getAsMap();
    }

    @Override
    public SortedSet<String> getNames() {
        return delegate.getNames();
    }

    @Override
    public T findByName(String name) {
        return delegate.findByName(name);
    }

    @Override
    public Rule addRule(Rule rule) {
        return delegate.addRule(rule);
    }

    @Override
    public Rule addRule(String description, Closure ruleAction) {
        return delegate.addRule(description, ruleAction);
    }

    @Override
    public List<Rule> getRules() {
        return delegate.getRules();
    }

    @Override
    public <S extends T> DomainObjectCollection<S> withType(Class<S> type, Action<? super S> configureAction) {
        return delegate.withType(type, configureAction);
    }

    @Override
    public <S extends T> DomainObjectCollection<S> withType(Class<S> type, Closure configureClosure) {
        return delegate.withType(type, configureClosure);
    }

    @Override
    public Action<? super T> whenObjectAdded(Action<? super T> action) {
        return delegate.whenObjectAdded(action);
    }

    @Override
    public void whenObjectAdded(Closure action) {
        delegate.whenObjectAdded(action);
    }

    @Override
    public Action<? super T> whenObjectRemoved(Action<? super T> action) {
        return delegate.whenObjectRemoved(action);
    }

    @Override
    public void whenObjectRemoved(Closure action) {
        delegate.whenObjectRemoved(action);
    }

    @Override
    public void all(Action<? super T> action) {
        delegate.all(action);
    }

    @Override
    public void all(Closure action) {
        delegate.all(action);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return delegate.iterator();
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <R> R[] toArray(R[] a) {
        return delegate.toArray(a);
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return delegate.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return delegate.retainAll(c);
    }

    @Override
    public void clear() {
        delegate.clear();
    }
}
