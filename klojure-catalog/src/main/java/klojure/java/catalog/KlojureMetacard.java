package klojure.java.catalog;

import clojure.lang.RT;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class KlojureMetacard extends MetacardImpl implements Map {

  public KlojureMetacard() {
    super();
  }

  public KlojureMetacard(Metacard metacard) {
    super(metacard);
  }

  @Override
  public int size() {
    return this.keySet().size();
  }

  @Override
  public boolean isEmpty() {
    return this.size() == 0;
  }

  @Override
  public boolean containsKey(Object key) {
    String keyName = (String) RT.var("clojure.core", "name")
        .applyTo(RT.seq(Arrays.asList(key)));

    Attribute attribute = this.getAttribute(keyName);

    return attribute != null;
  }

  @Override
  public boolean containsValue(Object value) {
    return this.values().contains(value);
  }

  @Override
  public Object get(Object key) {
    String keyName = (String) RT.var("clojure.core", "name")
        .applyTo(RT.seq(Arrays.asList(key)));

    MetacardType metacardType = this.getMetacardType();

    AttributeDescriptor descriptor = metacardType.getAttributeDescriptor(keyName);

    if (descriptor == null) {
      return null;
    }

    Attribute attribute = this.getAttribute(keyName);

    if (attribute == null) {
      return null;
    }

    return descriptor.isMultiValued()
        ? attribute.getValues()
        : attribute.getValue();
  }

  @Override
  public Object put(Object key, Object value) {
    return null;
  }

  @Override
  public Object remove(Object key) {
    return null;
  }

  @Override
  public void putAll(Map map) {

  }

  @Override
  public void clear() {

  }

  @Override
  public Set keySet() {
    MetacardType metacardType = this.getMetacardType();
    return metacardType.getAttributeDescriptors()
        .stream()
        .map((descriptor) -> descriptor.getName())
        .filter((key) -> this.containsKey(key))
        .map((key) -> {
          Object keyword = RT.var("clojure.core", "keyword")
              .applyTo(RT.seq(Arrays.asList(key)));
          return keyword;
        })
        .collect(Collectors.toSet());
  }

  @Override
  public Collection values() {
    return (Collection) this.keySet().stream()
        .map((key) -> this.get(key))
        .collect(Collectors.toList());
  }

  @Override
  public Set<Entry> entrySet() {
    return (Set<Entry>) this.keySet().stream()
        .map((key) -> new AbstractMap.SimpleEntry(key, this.get(key)))
        .collect(Collectors.toSet());
  }

}
