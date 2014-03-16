/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gson;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <p>Use this builder to construct a {@link Gson} instance when you need to set configuration
 * options other than the default. For {@link Gson} with default configuration, it is simpler to
 * use {@code new Gson()}. {@code GsonBuilder} is best used by creating it, and then invoking its
 * various configuration methods, and finally calling create. Here is an example:</p>
 * <pre>
 * Gson gson = new GsonBuilder();
 *     .setVersion(1.0)
 *     .setPrettyPrinting()
 *     .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
 *     .registerTypeAdapter(Id.class, new IdTypeAdapter())
 *     .create();
 * </pre>
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public final class GsonBuilder {

  private double ignoreVersionsAfter;
  private ModifierBasedExclusionStrategy modifierBasedExclusionStrategy;
  private final InnerClassExclusionStrategy innerClassExclusionStrategy;
  private boolean excludeFieldsWithoutExposeAnnotation;
  private final TypeAdapter typeAdapter;
  private JsonFormatter formatter;
  private FieldNamingStrategy fieldNamingPolicy;
  private final Map<Type, InstanceCreator<?>> instanceCreators;
  private final Map<Type, JsonSerializer<?>> serializers;
  private final Map<Type, JsonDeserializer<?>> deserializers;

  /**
   * Creates a GsonBuilder instance that can be used to build Gson with various configuration
   * settings. GsonBuilder follows the builder pattern, and it is typically used by first
   * invoking various configuration methods to set desired options, and finally calling
   * {@link #create()}. Here is an example:
   * <pre>
   * Gson gson = new GsonBuilder()
   *     .setVersion(1.0)
   *     .setPrettyPrinting()
   *     .registerTypeAdapter(Id.class, new IdTypeAdapter())
   *     .create();
   * </pre>
   * The order of invocation of configuration methods does not matter.
   */
  public GsonBuilder() {
    // setup default values
    ignoreVersionsAfter = VersionConstants.IGNORE_VERSIONS;
    innerClassExclusionStrategy = new InnerClassExclusionStrategy();
    modifierBasedExclusionStrategy = Gson.DEFAULT_MODIFIER_BASED_EXCLUSION_STRATEGY;
    excludeFieldsWithoutExposeAnnotation = false;
    typeAdapter = Gson.DEFAULT_TYPE_ADAPTER;
    formatter = Gson.DEFAULT_JSON_FORMATTER;
    fieldNamingPolicy = Gson.DEFAULT_NAMING_POLICY;
    instanceCreators = new LinkedHashMap<Type, InstanceCreator<?>>();
    serializers = new LinkedHashMap<Type, JsonSerializer<?>>();
    deserializers = new LinkedHashMap<Type, JsonDeserializer<?>>();
  }

  /**
   * Configures Gson to enable versioning support.
   *
   * @param ignoreVersionsAfter any field or type marked with a version higher than this value
   * are ignored during serialization or deserialization.
   * @return GsonBuilder to apply the Builder pattern.
   */
  public GsonBuilder setVersion(double ignoreVersionsAfter) {
    this.ignoreVersionsAfter = ignoreVersionsAfter;
    return this;
  }

  /**
   * Configures Gson to excludes all class fields that have the specified modifiers. By default,
   * Gson will exclude all fields marked transient or static. This method will override that
   * behavior.
   *
   * @param modifiers the field modifiers. You must use the modifiers specified in the
   *        {@link java.lang.reflect.Modifier} class. For example,
   *        {@link java.lang.reflect.Modifier#TRANSIENT},
   *        {@link java.lang.reflect.Modifier#STATIC}.
   * @return GsonBuilder to apply the Builder pattern.
   */
  public GsonBuilder excludeFieldsWithModifiers(int... modifiers) {
    boolean skipSynthetics = true;
    modifierBasedExclusionStrategy = new ModifierBasedExclusionStrategy(skipSynthetics, modifiers);
    return this;
  }

  /**
   * Configures Gson to exclude all fields from consideration for serialization or deserialization
   * that do not have the {@link com.google.gson.annotations.Expose} annotation.
   *
   * @return GsonBuilder to apply the Builder pattern.
   */
  public GsonBuilder excludeFieldsWithoutExposeAnnotation() {
    excludeFieldsWithoutExposeAnnotation = true;
    return this;
  }

  /**
   * Configures Gson to apply a specific naming policy to an object's field during serialization
   * and deserialization.
   *
   * @param namingConvention the JSON field naming convention to use for
   *        serialization/deserializaiton
   * @return GsonBuilder to apply the Builder pattern.
   */
  public GsonBuilder setFieldNamingPolicy(FieldNamingPolicy namingConvention) {
    return setFieldNamingStrategy(namingConvention.getFieldNamingPolicy());
  }

  /**
   * Configures Gson to apply a specific naming policy strategy to an object's field during
   * serialization and deserialization.
   *
   * @param fieldNamingPolicy the actual naming strategy to apply to the fields
   * @return GsonBuiler to apply the Builder pattern.
   */
  private GsonBuilder setFieldNamingStrategy(FieldNamingStrategy fieldNamingPolicy) {
    this.fieldNamingPolicy = new SerializedNameAnnotationInterceptingNamingPolicy(fieldNamingPolicy);
    return this;
  }

  /**
   * Configures Gson to output Json that fits in a page for pretty printing. This option only
   * affects Json serialization.
   *
   * @return GsonBuilder to apply the Builder pattern.
   */
  public GsonBuilder setPrettyPrinting() {
    setFormatter(new JsonPrintFormatter());
    return this;
  }

  /**
   * Configures Gson with a new formatting strategy other than the default strategy. The default
   * strategy is to provide a compact representation that eliminates all unneeded white-space.
   *
   * @param formatter the new formatter to use.
   * @see JsonPrintFormatter
   * @return GsonBuilder to apply the Builder pattern.
   */
  GsonBuilder setFormatter(JsonFormatter formatter) {
    this.formatter = formatter;
    return this;
  }

  /**
   * Configures Gson for custom serialization or deserialization. This method combines the
   * registration of an {@link InstanceCreator}, {@link JsonSerializer}, and a
   * {@link JsonDeserializer}. It is best used when a single object typeAdapter implements all the
   * required interfaces for custom serialization with Gson. If an instance creator, serializer or
   * deserializer was previously registered for the specified class, it is overwritten.
   *
   * @param typeOfT The class definition for the type T.
   * @param typeAdapter This object must implement at least one of the {@link InstanceCreator},
   *        {@link JsonSerializer}, and a {@link JsonDeserializer} interfaces.
   * @return GsonBuilder to apply the Builder pattern.
   */
  public GsonBuilder registerTypeAdapter(Type typeOfT, Object typeAdapter) {
    Preconditions.checkArgument(typeAdapter instanceof JsonSerializer
        || typeAdapter instanceof JsonDeserializer || typeAdapter instanceof InstanceCreator);
    if (typeAdapter instanceof InstanceCreator) {
      registerInstanceCreator(typeOfT, (InstanceCreator<?>) typeAdapter);
    }
    if (typeAdapter instanceof JsonSerializer) {
      registerSerializer(typeOfT, (JsonSerializer<?>) typeAdapter);
    }
    if (typeAdapter instanceof JsonDeserializer) {
      registerDeserializer(typeOfT, (JsonDeserializer<?>) typeAdapter);
    }
    return this;
  }

  /**
   * Configures Gson to use a custom {@link InstanceCreator} for the specified class. If an
   * instance creator was previously registered for the specified class, it is overwritten. You
   * should use this method if you want to register a single instance creator for all generic types
   * mapping to a single raw type. If you want different handling for different generic types of a
   * single raw type, use {@link #registerInstanceCreator(Type, InstanceCreator)} instead.
   *
   * @param <T> the type for which instance creator is being registered.
   * @param classOfT The class definition for the type T.
   * @param instanceCreator the instance creator for T.
   * @return GsonBuilder to apply the Builder pattern.
   */
  <T> GsonBuilder registerInstanceCreator(Class<T> classOfT,
      InstanceCreator<? extends T> instanceCreator) {
    return registerInstanceCreator((Type) classOfT, instanceCreator);
  }

  /**
   * Configures Gson to use a custom {@link InstanceCreator} for the specified type. If an instance
   * creator was previously registered for the specified class, it is overwritten. Since this method
   * takes a type instead of a Class object, it can be used to register a specific handler for a
   * generic type corresponding to a raw type. If you want to have common handling for all generic
   * types corresponding to a raw type, use {@link #registerInstanceCreator(Class, InstanceCreator)}
   * instead.
   *
   * @param <T> the type for which instance creator is being registered.
   * @param typeOfT The Type definition for T.
   * @param instanceCreator the instance creator for T.
   * @return GsonBuilder to apply the Builder pattern.
   */
  <T> GsonBuilder registerInstanceCreator(Type typeOfT,
      InstanceCreator<? extends T> instanceCreator) {
    instanceCreators.put(typeOfT, instanceCreator);
    return this;
  }

  /**
   * Configures Gson to use a custom JSON serializer for the specified class. You should use this
   * method if you want to register a common serializer for all generic types corresponding to a
   * raw type. If you want different handling for different generic types corresponding to a raw
   * type, use {@link #registerSerializer(Type, JsonSerializer)} instead.
   *
   * @param <T> the type for which the serializer is being registered.
   * @param classOfT The class definition for the type T.
   * @param serializer the custom serializer.
   * @return GsonBuilder to apply the Builder pattern.
   */
  <T> GsonBuilder registerSerializer(Class<T> classOfT, JsonSerializer<T> serializer) {
    return registerSerializer((Type) classOfT, serializer);
  }

  /**
   * Configures Gson to use a custom JSON serializer for the specified type. You should use this
   * method if you want to register different serializers for different generic types corresponding
   * to a raw type. If you want common handling for all generic types corresponding to a raw type,
   * use {@link #registerSerializer(Class, JsonSerializer)} instead.
   *
   * @param <T> the type for which the serializer is being registered.
   * @param typeOfT The type definition for T.
   * @param serializer the custom serializer.
   * @return GsonBuilder to apply the Builder pattern.
   */
  <T> GsonBuilder registerSerializer(Type typeOfT, final JsonSerializer<T> serializer) {
    serializers.put(typeOfT, serializer);
    return this;
  }

  /**
   * Configures Gson to use a custom JSON deserializer for the specified class. You should use this
   * method if you want to register a common deserializer for all generic types corresponding to a
   * raw type. If you want different handling for different generic types corresponding to a raw
   * type, use {@link #registerDeserializer(Type, JsonDeserializer)} instead.
   *
   * @param <T> the type for which the deserializer is being registered.
   * @param classOfT The class definition for the type T.
   * @param deserializer the custom deserializer.
   * @return GsonBuilder to apply the Builder pattern.
   */
  <T> GsonBuilder registerDeserializer(Class<T> classOfT, JsonDeserializer<T> deserializer) {
    return registerDeserializer((Type) classOfT, deserializer);
  }

  /**
   * Configures Gson to use a custom JSON deserializer for the specified type. You should use this
   * method if you want to register different deserializers for different generic types
   * corresponding to a raw type. If you want common handling for all generic types corresponding to
   * a raw type, use {@link #registerDeserializer(Class, JsonDeserializer)} instead.
   *
   * @param <T> the type for which the deserializer is being registered.
   * @param typeOfT The type definition for T.
   * @param deserializer the custom deserializer.
   * @return GsonBuilder to apply the Builder pattern.
   */
  <T> GsonBuilder registerDeserializer(Type typeOfT, final JsonDeserializer<T> deserializer) {
    deserializers.put(typeOfT, deserializer);
    return this;
  }

  /**
   * Creates a {@link Gson} instance based on the current configuration.
   *
   * @return an instance of Gson configured with the options currently set in this builder.
   */
  public Gson create() {
    List<ExclusionStrategy> strategies = new LinkedList<ExclusionStrategy>();
    strategies.add(innerClassExclusionStrategy);
    strategies.add(modifierBasedExclusionStrategy);
    if (ignoreVersionsAfter != VersionConstants.IGNORE_VERSIONS) {
      strategies.add(new VersionExclusionStrategy(ignoreVersionsAfter));
    }
    if (excludeFieldsWithoutExposeAnnotation) {
      strategies.add(new ExposeAnnotationBasedExclusionStrategy());
    }
    ExclusionStrategy exclusionStrategy = new DisjunctionExclusionStrategy(strategies);
    ObjectNavigatorFactory objectNavigatorFactory =
        new ObjectNavigatorFactory(exclusionStrategy, fieldNamingPolicy);
    MappedObjectConstructor objectConstructor = new MappedObjectConstructor();
    Gson gson = new Gson(
        objectNavigatorFactory, objectConstructor, typeAdapter, formatter);

    for (Map.Entry<Type, JsonSerializer<?>> entry : serializers.entrySet()) {
      gson.registerSerializer(entry.getKey(), entry.getValue());
    }

    for (Map.Entry<Type, JsonDeserializer<?>> entry : deserializers.entrySet()) {
      gson.registerDeserializer(entry.getKey(), entry.getValue());
    }

    for (Map.Entry<Type, InstanceCreator<?>> entry : instanceCreators.entrySet()) {
      gson.registerInstanceCreator(entry.getKey(), entry.getValue());
    }
    return gson;
  }
}
