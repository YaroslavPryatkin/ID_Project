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

---

## 4. Keyboard Shortcuts and Navigation

The application provides comprehensive keyboard-driven navigation and data manipulation capabilities to maximize productivity. Shortcuts are context-sensitive and adapt based on the current view state (input form, result table, or console).

### 4.1. Screen and Function Navigation

These shortcuts allow rapid navigation between screens and functions without using the mouse.

| Shortcut | Action |
|----------|--------|
| **Alt + ↑** | Switch to previous function in the current screen list |
| **Alt + ↓** | Switch to next function in the current screen list |
| **Alt + ←** | Switch to previous screen |
| **Alt + →** | Switch to next screen |

*Note:* Navigation wraps around—pressing down arrow at the last function cycles to the first, and vice versa.

### 4.2. Function Execution and Form Control

These shortcuts manage the execution workflow for the currently selected function.

| Shortcut | Action |
|----------|--------|
| **Alt + R** | Always toggle console visibility |
| **Alt + Q** | Triggers the first visible button in the bottom toolbar |
| **Alt + W** | Triggers the second visible button in the bottom toolbar |
| **Alt + E** | Triggers the third visible button in the bottom toolbar |

#### Button-Linked Shortcut Availability

The Alt + Q, W, E shortcuts are dynamically mapped to visible buttons in the bottom toolbar:

* If only **1 button** is visible → Only **Alt + Q** works
* If **2 buttons** are visible → **Alt + Q** and **Alt + W** work
* If **3 buttons** are visible → **Alt + Q**, **Alt + W**, and **Alt + E** work

The toolbar buttons adapt based on context (input form vs. result view vs. console), so available shortcuts change accordingly.

#### Console Toggle

**Alt + R** is independent and always available—it universally toggles the console visibility regardless of other view state or available buttons.

### 4.3. Console and Result View Navigation

These shortcuts manage the display of result data and console output.

| Shortcut | Action |
|----------|--------|
| **Enter** | Return from console or result table view to input form |
| **Escape** | Return from console or result table view to input form |

### 4.4. Table Data Selection and Copying

When a function returns tabular data, the result view provides keyboard-driven row selection for quick data extraction.

| Shortcut | Action |
|----------|--------|
| **↑ / ↓** | Navigate up/down through table rows |
| **Shift + ↑ / Shift + ↓** | Extend row selection to adjacent rows |
| **← / →** | Enter cell selection mode; navigate left/right through cells (wraps to previous/next row at boundaries) |
| **Shift + ← / Shift + →** | Extend cell selection to adjacent cells |
| **Ctrl + C** | Copy selected rows or cells to system clipboard |
| **Escape** | Return from result table view to input form |

### 4.5. Moving Through Parameters

#### 4.5.1. Moving Down

When focused on a parameter input field, you can move to the next parameter using:

| Shortcut | Behavior |
|----------|----------|
| **Enter** | Always moves to the next parameter field (or executes function if at the last parameter) |
| **Tab** | Moves to the next parameter field, BUT only if a dropdown is not open. If a dropdown is visible, Tab selects the highlighted item and closes the dropdown. |

#### 4.5.2. Moving Up

To navigate backward through parameters:

| Shortcut | Behavior |
|----------|----------|
| **↑** (Up Arrow) | Always moves to the previous parameter field |
| **Backspace** | Moves to the previous parameter field, BUT only if the current input field is empty. If the field has content, Backspace deletes the last character (normal behavior). |
| **Shift + Tab** | Moves to the previous parameter field, BUT only if a dropdown is not open. If a dropdown is visible, Shift+Tab selects the highlighted item and closes the dropdown. |

### 4.6 Dropdown List Keyboard Control

When a dropdown list is open and has focus, these shortcuts apply:

| Shortcut | Action                                                                                                           |
|----------|------------------------------------------------------------------------------------------------------------------|
| **↑ / ↓** | Navigate up/down through dropdown options (wraps around at boundaries)                                           |
| **Tab** | Select currently highlighted option. Does NOT move to next parameter.                                            |
| **Escape** | Close dropdown without making a selection                                                                        |
| **Enter** | Select currently highlighted option, close dropdown, and move to next parameter (or execute if at last parameter) |

When a dropdown list is **closed**:

| Shortcut            | Action                                                |
|---------------------|-------------------------------------------------------|
| **↓** (Down Arrow)  | Open the dropdown list and highlight the first option |
| **Everything else** | Acts like dropdown doesn't exist                      |

### 4.7 The Enter Key: Detailed Behavior

The **Enter** key is context-aware and adapts based on the function and current state:

**Case 1: Function has no parameters**
- Enter immediately executes the function (equivalent to pressing Alt+Q or clicking the Apply button)

**Case 2: Function has parameters, but none is focused**
- Enter moves focus to the first parameter field

**Case 3: A non-last parameter is focused**
- Enter moves focus to the next parameter field
- If the current parameter has a dropdown open, the highlighted item is selected first, then focus moves

**Case 4: The last parameter is focused**
- Enter executes the function with all parameter values
- On successful execution, all input fields are automatically cleared
- If the last parameter has a dropdown open, the highlighted item is selected first, then the function executes

**Case 5: Console or result table is displayed**
- Enter closes the console/result view and returns to the parameter input form

### 4.8 Typical Parameter Input Workflow

1. Switch between screens using **Alt + ← / Alt + →**
2. Select a function using **Alt + ↑ / Alt + ↓**
3. Press **Enter** to focus the first parameter field
4. Type a value for the parameter
5. Press **Enter** to move to the next parameter
6. For dropdown parameters, press ↓ to open the list, use ↑/↓ to navigate, and press Enter to select and move to next parameter
7. Repeat steps 4-6 for each parameter
8. On the last parameter, press **Enter** to execute the function
9. View results
10. Press **Enter** to close the results