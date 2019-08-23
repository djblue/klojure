package klojure;

import clojure.lang.PersistentArrayMap;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.impl.types.LocationAttributes;
import ddf.catalog.data.types.Core;
import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ClojureMetacard implements Metacard {

  private PersistentArrayMap map;

  private AttributeRegistry registry;

  public ClojureMetacard(PersistentArrayMap map, AttributeRegistry registry) {
    this.map = map;
  }

  @Override
  public Attribute getAttribute(String name) {
    return new Attribute() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public Serializable getValue() {
        return getAs(name, Serializable.class);
      }

      @Override
      public List<Serializable> getValues() {
        Object value = getAs(name, Object.class);
        if (value instanceof List) {
          return (List) value;
        }
        if (value instanceof Serializable) {
          return Collections.singletonList((Serializable) value);
        }
        return null;
      }
    };
  }

  @Override
  public void setAttribute(Attribute attribute) {

  }

  @Override
  public MetacardType getMetacardType() {
    Set<AttributeDescriptor> descriptors = (Set<AttributeDescriptor>) map.keySet().stream()
        .map((key) -> registry.lookup((String) key).get())
        .collect(Collectors.toSet());

    return new MetacardTypeImpl("clojure.metacard", descriptors);
  }

  private <T> T getAs(String key, Class<T> type) {
    Object value = this.map.get(key);
    if (type.isInstance(value)) {
      return type.cast(value);
    }
    return null;
  }

  @Override
  public String getId() {
    return getAs(Metacard.ID, String.class);
  }

  @Override
  public String getMetadata() {
    return getAs(Metacard.METADATA, String.class);
  }

  @Override
  public Date getCreatedDate() {
    return getAs(Metacard.CREATED, Date.class);
  }

  @Override
  public Date getModifiedDate() {
    return getAs(Metacard.MODIFIED, Date.class);
  }

  @Override
  public Date getExpirationDate() {
    return getAs(Metacard.EXPIRATION, Date.class);
  }

  @Override
  public Date getEffectiveDate() {
    return getAs(Metacard.EFFECTIVE, Date.class);
  }

  @Override
  public String getLocation() {
    return getAs(Core.LOCATION, String.class);
  }

  @Override
  public String getSourceId() {
    return getAs(Core.SOURCE_ID, String.class);
  }

  @Override
  public void setSourceId(String s) {

  }

  @Override
  public String getTitle() {
    return getAs(Metacard.TITLE, String.class);
  }

  @Override
  public URI getResourceURI() {
    return getAs(Metacard.RESOURCE_URI, URI.class);
  }

  @Override
  public String getResourceSize() {
    return getAs(Metacard.RESOURCE_SIZE, String.class);
  }

  @Override
  public byte[] getThumbnail() {
    return getAs(Metacard.THUMBNAIL, byte[].class);
  }

  @Override
  public String getContentTypeName() {
    return getAs(Metacard.CONTENT_TYPE, String.class);
  }

  @Override
  public String getContentTypeVersion() {
    return getAs(Metacard.CONTENT_TYPE_VERSION, String.class);
  }

  @Override
  public URI getContentTypeNamespace() {
    //return getAs(Core., URI.class);
    return null;
  }
}
