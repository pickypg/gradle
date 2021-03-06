
Currently much of the software component model is not managed, and so is not visible to rules and cannot take any real
advantage of the features of that rule based configuration offers.

This spec outlines several steps toward a fully managed software component model.

# Feature 4: Plugin author uses managed types to extend the software model

This feature allows a plugin author to extend certain key types using a managed type:

- `ComponentSpec`
- `LanguageSourceSet`
- `BinarySpec`
- `JarBinarySpec`

It is a non-goal of this feature to add any further capabilities to managed types, or to migrate any of the existing subtypes to managed types.

## Implementation

This feature will require some state for a given object to be unmanaged, possibly attached as node private data, and some state to be managed, backed by
individual nodes.

As part of this work, remove empty subclasses of `BaseLanguageSourceSet`, such as `DefaultCoffeeScriptSourceSet`.

## Story: Custom JarBinarySpec type is implemented as a @Managed type

Specific support will be added for specialisations of `JarBinarySpec`.
This is being targeted first as it is needed to continue the dependency management stream.

### Implementation

(unordered)

- Allow plugins to register schema extraction strategies via our plugin service registry mechanism
- Have the platformJvm module register a strategy for `JarBinarySpec` subtypes
- Change `@BinaryType` rule to have some understanding of `@Managed` types so that `defaultImplementation` is not required or allowed for managed binary types.
- Add support for having managed types be somehow backed by a “real object”

> For this story, the simplest approach may be to do this via delegation.
> First we create a `JarBinarySpecInternal` using the existing machinery, then create a managed subtype wrapper that delegates
> all `JarBinarySpecInternal` to the manually created unmanaged instance.
> This will require opening up the type generation mechanics to some extent.

- Open up managed node creation mechanics to allow “custom” strategies

> The schema extraction strategy may be the link here.
> That is, the strategy for creating the _node_ may be part of the schema.

- Implementation of `@BinaryType` rule WRT interaction with `BinarySpecFactory` will need to be “managed aware”
- Move `baseName` property from `JarBinarySpecInternal` to `JarBinarySpec`

### Tests

- Illegal managed subtype registered via `@BinaryType` yields error at rule execution time (i.e. when the binary types are being discovered)
- Attempt to call `binaryTypeBuilder.defaultImplementation` fails eagerly if public type is `@Managed`
- Subtype can have `@Unmanaged` properties
- Subtype can have further subtypes
- Subtype exhibits managed impl behaviour WRT immutability when realised
- Subtype can be cast and used as `BinarySpecInternal`
- Subtype cannot be created via `BinaryContainer` (i.e. top level `binaries` node) - (requires node backing)
- Can successfully create binary represented by `JarBinarySpec` subtype

# Feature 5: Plugin author declares internal views for model element types

Allow a plugin author to declare internal views for a particular type.

## Plugin author declares internal view for custom component type

Given a `ComponentSpec` subtype with default implementation extending `BaseComponentSpec`, allow one or more internal views to be
registered when the component type is registered:

    @ComponentType
    public void registerMyType(ComponentTypeBuilder<MyType> builder) {
        builder.defaultImplementation(MyTypeImpl.class);
        builder.internalView(MyTypeInternal.class);
        builder.internalView(MyInternalThing.class);
    }

- Each internal view must be an interface. The interface does not need to extend the public type.
- For this story, the default implementation must implement each of the internal view types. Fail at registration if this is not the case.
- Model report shows elements as public view type. Internal views and properties defined in them are not shown in the report for this story.
- Internal view type can be used with `ComponentSpecContainer` methods that filter components by type, eg can do `components { withType(MyTypeInternal) { ... } }`.
- Rule subjects of type `ModelMap<>` can be used to refer to nodes by an internal view.

### Test cases

- Internal view type can be used with `ComponentSpecContainer.withType()`.
    - a) if internal view extends public view
    - b) if internal view does not extend public view
- Internal view type can be used with rule subjects like `ModelMap<InternalView>`.
    - a) if internal view extends public view
    - b) if internal view does not extend public view
- Model report shows only public view type properties.
- Error cases:
    - Non-interface internal view raises error during rule execution time.
    - Default implementation type that does not implement public view type raises error.
    - Default implementation type that does not implement all internal view types raises error.
    - Specifying the same internal view twice raises an error.


## Plugin author declares internal view for custom binary and source set types

Generalise the previous story to also work with `BinarySpec` and `LanguageSourceSet` subtypes that provide a default implementation.

### Implementation

- Should start to unify the type registration infrastructure, so that registration for all types are treated the same way and there are few or no differences
between the implementation of component, binary and source set type registration rules. This will be required for the next stories.

## Plugin author declares internal views for extensible type

Given a plugin defines a general purpose type that is then extended by another plugin, allow internal views to be declared for the general type as well as the
specialized type. For example:

    class BasePlugin extends RuleSource {
        @ComponentType
        public void registerBaseType(ComponentTypeBuilder<BaseType> builder) {
            builder.internalView(BaseTypeInternal.class);
        }
    }

    interface CustomType extends BaseType { }

    class CustomPlugin extends RuleSource {
        @ComponentType
        public void registerCustomType(ComponentTypeBuilder<CustomType> builder) {
            builder.internalView(MyCustomTypeInternal.class);
        }
    }

The views defined for the general type should also be applied to the specialized type. So, in the example above, every instance of `CustomType` should have the
`BaseTypeInternal` view applied to it.

- Allow for all types that support registration.
- Change all usages of `@ComponentType` and `@BinaryType` in core plugins to declare internal view types.
- Add a rule to the base plugins, to declare internal view types for `ComponentSpec` and `BinarySpec`.
- Change node creation so that implementation is no longer available as a view type.

## Plugin author declares default implementation for extensible type

Given a plugin defines a general type, allow the plugin to provide a default implementation the general type.
This default implementation is then used as the super class for all `@Managed` subtype of the general type. For example:

    class BasePlugin extends RuleSource {
        @ComponentType
        public void registerBaseType(ComponentTypeBuilder<BaseType> builder) {
            builder.defaultImplementation(BaseTypeInternal.class);
        }
    }

    @Managed
    interface CustomType extends BaseType { }

    class CustomPlugin extends RuleSource {
        @ComponentType
        public void registerCustomType(ComponentTypeBuilder<CustomType> builder) {
            // No default implementation required
        }
    }

- Generalise the work done to allow `@Managed` subtypes of `JarBinarySpec` to support this.
- Allow for all types that support registration.
- Change core plugins to declare default implementations for `ComponentSpec`, `BinarySpec` and `LanguageSourceSet`. This will allow `@Managed` subtypes of each
of these types.

## Plugin author declares internal views for custom managed component type

Given a plugin defines a `@Managed` subtype of a general type, allow the plugin to define internal views for that type.

- Allow for all types that support registration.
- Each internal view must be an interface. The interface does not need to extend the public type.
- Each internal view must be `@Managed` when no default implementation is declared.
- Allow an internal view to make a property mutable.
- Allow an internal view to specialize the type of a property. This implicitly adds a view to the property node.
- Allow an internal view to declare additional properties for a node. These properties should be hidden.
- Generate a proxy type for each view type.
- Remove constraint the default implementation should implement the internal view types. Instead, use the proxy type.
- toString() and missing property/method error messages should reflect view type rather than implementation type, for generated views.

## Plugin author declares internal views for any managed type

Allow a rule to declare internal views for any `@Managed` type.

## Model report does not show internal properties of an element

Infer a model element's hidden properties based on the parent's views:

- When a property is declared on any of the parent's public view types, that property should be considered public.
- When a property is declared only on the parent's internal view types, that property should be considered hidden and not shown.
- Add an option to model report to show all hidden elements and types.

# Feature 6: Managed Model usability

Some candidates:

- Consistent validation when managed type, ModelMap and ModelSet are used as inputs.
- Consistent validation when managed type, ModelMap and ModelSet are mutated after view is closed.
- Consistent validation when managed type, ModelMap and ModelSet used on subject that is not mutable.
- Consistent error message for ModelMap<T> and ModelSet<T> where T is not managed.
- Consistent usage of ModelMap and ModelSet with reference properties.
- Consistent mutation methods on ModelMap and ModelSet.
- Enforce that `@Defaults` rules cannot be applied to an element created using non-void `@Model` rule.
- Enforce that subject cannot be mutated in a validate rule.
- Rename (via add-deprecate-remove) the mutation methods on ModelMap and ModelSet to make more explicit that
  they are intended to be used to define mutation rules that are invoked later. For example `all { }` or `withType(T) { }` could have better names.
- Add methods (or a view) that allows iteration over collection when it is immutable.
- Rename (via add-deprecate-remove) `@Mutate` to `@Configure`.
- Allow empty managed subtypes of ModelSet and ModelMap. This is currently available internally, eg for `ComponentSpecContainer`.

# Feature 7: Build author applies cross-cutting rules for software model types

Link all `ComponentSpec`, `LanguageSourceSet`, `BinarySpec` and `Task` instances into top level containers where rules can be
applied to them regardless of their location in the model.

Non-goal is to provide this as a general capability for arbitrary types.

## Referenced element can be used as input for a rule

For example:

    @Managed
    interface Person {
        Address getAddress()
        void setAddress(Address address)
    }

    model {
        person(Person) {
            address = $(...)
        }
        delivery {
            sendTo = $(person.address)
            destinationCity = $(person.address.city)
        }
    }

- When binding a path for input, need to realize enough of each element to finalize references so that references can be traversed.
- Need to handle paths that traverse a `null` reference.
- Error messages on binding failures.
- Reference value can be changed while mutation is allowed. Treat reference change as remove and add.
- Can't remove an element when it is the target of a reference.

### Test cases

- Can bind to target element via reference path.
- Nice error message when reference is `null`. Should indicate where the `null` reference is.
- Nice error message when binding to unknown child element.
- Can bind to child of target element via reference path.
- When reference is attached in `@Defaults` rule, configuration rules are applied to target element.
- Can bind element via path that contains several references.
- Can reference to ancestor.
- Can mutate reference.

### Backlog

- Deal with case where by-path binding points to a null reference or null scala value, rather than traverses a null reference. 
- Deal with by-type binding to a non-null reference.

## Model report shows references between elements

- Creator and mutator rules should be those that affected the value of the reference, not the target.

### Test cases

- Reference is `null`.
- Reference is not `null`.
- Can mutate reference value during configuration.
- Cycle from child to parent.

## Referenced element can be used as subject for a rule

- For defaults, finalization and validation rules.
- Can only be applied when the target of the reference still allows these rules to be applied.
- Error messages when rule cannot be applied.
- Out of scope: locating referencing elements in the model, in order to inject rules via the references. This is intended to be used internally
only to implement the top level containers.

## Model containers allow elements to be added as references

- Adding a managed element to a model container should be treated as adding a reference to the target element.

## Language source sets are linked into top level container

- Change `LanguageSourceSet` implementations so that they are node backed.
- Apply the above capabilities to the `sources` top level container.

## Binaries are linked into top level container

- Change `BinarySpec` implementations so that they are node backed.
- Apply the above capabilities to the `binaries` top level container.

## Backlog

- Apply to `ComponentSpec`, `Task`, etc.
- Conveniences to apply rules to any thing of a given type.

# Feature 8: Plugin author attaches source sets to managed type

This feature allows source sets to be added to arbitrary locations in the model. For example, it should be possible to model Android
build types and flavors each with an associated source set.

It is also a goal of this feature to make `ComponentSpec.sources` and `BinarySpec.sources` model backed containers.

## Story: Allow `LanguageSourceSet` instances to be attached to a managed type

- Allow any registered subtype of `LanguageSourceSet` to be used as:
    - A read-only property of a `@Managed` type
    - An element of managed collections `ModelSet` and `ModelMap`
    - A top level element.
- TBD: Need some convention for source directory locations. Possibly add a `baseDir` property to `FunctionalSourceSet` and default source directories to `$baseDir/$sourceSet.name`
- TBD: Reporting changes, if any

- Out-of-scope: Instances are visible in top level `sources` container.

### Implementation

- Converge on `NodeInitializer` as the strategy for creating the children of a managed type, the elements of a model collection and top level elements:
    - Hoist this up to `ModelSchema`
    - Replace the various `ChildNodeInitializerStrategy` implementation with one that delegates to the schema.
    - Add some way to register a `NodeInitializer` for an unmanaged or partially managed type.
- Change validation for managed type properties and managed collection elements to allow any type for which a creation strategy is available.
- Add creation strategy for `LanguageSourceSet` backed by type registration.

## Story: Elements of binary `sources` container are visible to rules

### Implementation

- Use the same approach as used to make `ComponentSpec.sources` visible to rules.
    - Will need to make `BaseBinarySpec` node backed, similar to `BaseComponentSpec`.   
    - Should refactor to simplify both cases.

## Story: Allow `FunctionalSourceSet` instances to be attached to a managed type

- Empty by default.
- All registered source set types are available to be added.
- TBD: Change `FunctionalSourceSet` to extend `ModelMap`. Probably should be split out as a separate story.

### Implementation

- TBD: Currently `FunctionalSourceSet` pushes instances into `sources` container. Should change this to work the same way as binaries, where the owner of the binary has 
no knowledge of where its elements end up being referenced.
- TBD: Currently rules push language registrations into various well known instances. Should change this to work with all instances, ideally by pull rather than push.
- TBD: Currently `CUnitPlugin` uses methods on `FunctionalSourceSet` that are not available on `ModelMap`.
- TBD: Reuse `FunctionalSourceSet` for `ComponentSpec.sources` and `BinarySpec.sources`.

## Story: Elements of `sources` container are visible to rules 

- TBD: Change `ProjectSourceSet` so that it is bridged in the same way as the `binaries` container, alternatively move `sources` completely into model space.
- TBD: Currently `JavaBasePlugin` contributes source sets to `sources` container.

## Story: Build logic applies cross cutting configuration to all `LanguageSourceSet` instances 

- All `LanguageSourceSet` instances are visible through `sources` container
- Depends on improvements to reference handling define in previous feature. 
- TBD: Need to traverse schema to determine where source sets may be found in the model. Ensure only those model elements that are required are realized. 
- TBD: Need to split this up into several stories.

# Later features

# Feature: Key component model elements are not realized until required

This feature continues earlier work to make key properties of `BinarySpec` managed.

- `BinarySpec.tasks`

# Feature: Build logic defines tasks for generated source sets and intermediate outputs

This feature generalizes the infrastructure through which build logic defines the tasks that build a binary, and reuses it for generated source sets
and intermediate outputs.

A number of key intermediate outputs will be exposed for their respective binaries:

- Native object files
- JVM class files
- Generated source for play applications

Rules implemented either in a plugin or in the DSL will be able to define the tasks that build a particular binary from its intermediate outputs,
an intermediate output from its input source sets, or a particular source set. Gradle will take care of invoking these rules as required.

Rules will also be able to navigate from the model for a buildable item, such as a binary, intermediate output or source set, to the tasks, for
configuration or reporting.

The `components` report should show details of the intermediate outputs of a binary, the input relationships between the source sets, intermediate outputs and
binaries, plus the task a user would run to build each thing.

It is a non-goal of this feature to provide a public way for a plugin author to define the intermediate outputs for a binary. This is a later feature.

## Implementation

A potential approach:

- Introduce an abstraction that represents a physical thing, where a binary, a source set and intermediate outputs are all physical things.
- Allow the inputs to a physical thing to be modelled. These inputs are also physical things.
- Allow a rule, implemented in a plugin or in the DSL, to define the tasks that build the physical thing.
- Allow navigation from the model for the physical thing to the tasks that are responsible for building it.
- A pre-built physical thing will have no tasks associated with it.

Many of these pieces are already present, and the implementation would formalize these concepts and reuse them.

Currently:

- `BuildableModelElement` represents a physical thing.
- `BinarySpec.tasks` and properties on `BuildableModelElement` represent the tasks that build the thing.
- `BinarySpec.sources` represents the inputs to a binary.
- A `@BinaryTasks` rule defines the tasks that build the binary, as do various methods on `LanguageSourceSet` and `BuildableModelElement`.
- Various types, such as `JvmClasses` and `PublicAssets` represent intermediate outputs.

The implementation would be responsible for invoking the rules when assembling the task graph, so that:

- When a physical thing is to be built, the tasks that build its inputs should be configured.
- When a physical thing is used as input, the tasks that build its inputs, if any, should be determined and attached as dependencies of those tasks
that take the physical thing as input.

# Feature: Plugin author uses managed types to define intermediate outputs

This feature allows a plugin author to declare intermediate outputs for custom binaries, using custom types to represent these outputs.

Allow a plugin author to extend any buildable type with a custom managed type. Allow a custom type to declare the inputs for the buildable type in a strongly typed way.
For example, a JVM library binary might declare that it accepts any JVM classpath component as input to build a jar, where the intermediate classes directory is a
kind of JVM classpath component.

## Implementation

One approach is to use annotations to declare the roles of various strongly typed properties of a buildable thing, and use this to infer the inputs
of a buildable thing.

# Feature: Build logic defines tasks that run executable things

This feature generalizes the infrastructure through which build logic defines the executable things and how they are to be executed.

The goals for this feature:

- Introduce an abstraction that represents an executable thing, where an installed executable or a test suite variant are executable things.
- Allow a rule to define the tasks that run the executable thing.
- Allow navigation from the model for the executable thing to the tasks that run it.
- Expose an installed native executable and a test suite variant as executable things.

This feature should sync with the plan for play application execution.

## Implementation

The implementation would be responsible for building the executable things, and then configuring and running the appropriate tasks.

The `components` report should show details of the executable things, which as the entry point task to run the thing.

# Feature: References between key objects in the software model are visible to rules

The relationships exposed in the first feature represent ownership, where the relationship is one between a parent and child.
This feature exposes other key 'non-ownership' relationships present in the software model.

- A binary has a collection of language source sets that it takes as input. These are not owned by the binary, but form its inputs.
- A test suite component has component under test associated with it.
- The project level binaries collection is a collection of binaries owned by various components.
- The project level sources collection is a collection of language source sets owned by various components.
- The project level task collection is a collection of tasks owned by various binaries, source sets, and other buildable things.

At the completion of this feature, it should be possible to see the above relationships represented in the `model` report.

It is a non-goal of this feature to allow rules to be written to select these objects using their 'non-ownership' paths.

## Implementation

For a binary's input source sets, one option would be to change the behaviour so that a binary receives a copy of its component's source sets. These
copies would then be owned by the binary and can be further customized in the context of the binary.

For a test suite's component under test, one option would be to restructure the relationship, so that test suite(s) become a child of the component under test.
