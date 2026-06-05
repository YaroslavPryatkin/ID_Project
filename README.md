
# Universal PostgreSQL GUI Engine Configuration Documentation

This document describes the syntax and runtime behavior of the universal GUI dynamic configuration system. The system automatically inspects PostgreSQL database functions, maps them to graphical UI elements based on prefix matching, and uses this configuration file to override automatically generated labels with human-readable aliases and specialized input fields (dropdowns).

---

## 1. File Structure and Syntax

The configuration file is a line-oriented layout engine consisting of two main segments: database parameters and screen layout definitions.

### 1.1. Database Connection Block
The top of the file initializes the connection pool parameters using standard key-value pairs:
```yaml
url: [host:port]
database: [database name]
user: [username]
password: [password]
```

### 1.2. Layout Definition Block

The core layout is built using two main structural keywords: `screen:` and `fun:`.

#### The `screen:` Directive

Defines a main navigation section (such as a tab, panel, or menu) in the GUI.

```text
screen: [prefix] [color] [label]
```

* **`[prefix]`**: A database-side function name prefix (e.g., `read_functions`, `insert_functions`). The engine filters database functions using this prefix to determine which screen they belong to.
* **`[color]`**: A hexadecimal color code (e.g., `#5F8D4E`) used as the base accent theme for this specific screen.
* **`[label]`**: A multi-word, human-readable title displayed on the UI tab or header.

#### The `fun:` Directive

Overrides the visual mapping for a specific database function within the currently active screen.

```text
fun: [sql_name] [gui display name] ([parameter alias 1], [parameter alias 2], ...)
```

* **`[sql_name]`**: The exact name of the function inside the PostgreSQL database (excluding the screen's `[prefix]_`).
* **`[gui display name]`**: Custom multi-word text displayed on the trigger element (e.g., a button).
* **`([parameter aliases])`**: A comma-separated list inside parentheses that customizes the input form fields generated for the function parameters.

---

## 2. GUI Engine Runtime Logic

The engine uses a fallback strategy, combining metadata introspection directly from PostgreSQL with the styling rules provided in this configuration file.

### 2.1. Discovery and Grouping

1. The engine scans the PostgreSQL schema for available functions.
2. It gathers all functions matching the structure `[prefix]_[some name]`.
3. These functions are assigned to the respective screen declared with that `[prefix]`, using its `[label]` and `[color]` for UI rendering.

### 2.2. Rendering Function Controls (`fun:`)

When rendering a discovered function onto its screen, the engine checks if the function has an override rule defined in the configuration file:

* **Case 1: Function is NOT in the configuration file**
  The engine renders a button using the raw database function name. The input form fields are auto-generated using the raw parameter names and data types extracted directly from the database schema.
* **Case 2: Function IS defined in the configuration file**
  The engine replaces the raw database action name with your custom `[gui display name]`. It then maps the parameters inside `(...)` sequentially to the function's arguments.

#### Parameter Arity Fallback

If a database function requires 5 parameters, but your configuration rule only defines 3 aliases:

1. The first 3 parameters will use your custom aliases and component modifiers.
2. The remaining 2 parameters fallback to their default names and types defined in the database schema.

---

## 3. Dynamic UI Components & Modifiers

The engine evaluates configuration keywords to transform standard text input boxes into specialized interactive UI elements.

### 3.1. Dropdown Menus (Enums & Tables)

By default, fields display as regular text inputs using their given alias name. If an alias uses a technical prefix, it switches the input component type. **The system automatically strips these prefix keywords before displaying the final label to the user.**

* **`droplistenum_[enum_name] [Label]`**
  Transforms the input field into a dropdown combo box populated by the values of a database-defined Enum type (`[enum_name]`).
* **`droplisttable_[table_name] droplistfield_[field_name] [Label]`**
  Transforms the input field into a data-bound dropdown selection component. It queries the specified database `[table_name]`, fetches rows, and maps the selection back to the corresponding `[field_name]` key. 

In both cases the UI only displays `[Label]` to the user.

### 3.2. Output Suppression: The `noout_` Prefix

If a function performs a mutation (like an INSERT, UPDATE, or DELETE operation) and does not return data to be viewed in a data grid, it can use the explicit `noout_` keyword.

* **Syntax Requirement:** The `noout_` keyword must be explicitly written down inside the configuration file's function name field (e.g., `fun: noout_my_function ...`).
* **Behavior:** The GUI engine catches this prefix and suppresses any expected data return matrices, executing the procedure silently or treating it purely as a command invocation rather than a data query.
* *Note:* Unlike the screen `prefix`, which is omitted from the function line, `noout_` **must** be written down if present in the target database routine.


