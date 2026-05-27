# Config File Syntax (config.conf)



## Database Connection Parameters



```

url: localhost:5432

database: postgres

user: postgres

password: postgres

```



- **url**: PostgreSQL host and port (format: `host:port`)

- **database**: database name

- **user**: PostgreSQL username

- **password**: PostgreSQL password



An application itself may also change this fields.



## Function Screens



```

screen: read_functions #4F709C Reading data

screen: insert_functions #5F8D4E Inserting new data

screen: remove_functions #A75D5D Removing data

```



**Format:** `screen: prefix color display\_name`



- **prefix**: prefix of database functions associated with this screen

&#x20; - The application automatically finds all functions in the database that start with this prefix

&#x20; - All matching functions are grouped together on one screen

- **color**: screen color in hex format (e.g., `#4F709C`)

- **display\_name**: screen name displayed in the UI (can contain multiple words)



**Example database functions for prefix `read_functions`:**

```sql

read_functions_all_categories()

read_functions_products_by_category(category_id)

```



## Function Mapping and Renaming



```

fun: all_categories Show all categories

fun: products_by_category Show all products from the specified category (Id of the category)

```



**Format:** `fun: sqlName guiName [(alias1, alias2, ...)]`



- **sqlName\*\*: function name in the database (without the prefix)

- **guiName\*\*: how the function is displayed in the UI (can contain multiple words)

- **\[(alias1, alias2, ...)]** *(optional)*: parameter names displayed in the UI, in the order they appear in the function signature



**Example with aliases:**

```

fun: get_user_data Get User Info (User ID)

```

The first parameter of `get_user_data` will be displayed as "User ID" in the interface.



**Example with multiple aliases:**

```

fun: insert_product Add Product (Product Name, Price, Category)

```



## Special Prefix: noout



For functions that should not display output (e.g., INSERT, UPDATE, DELETE operations):



**Database function naming:** `prefix_noout_function_name`



Example:

```sql

insert_functions_noout_add_category(name)

remove_functions_noout_delete_product(product_id)

```



In the config file, these functions are specified **with** noout_ prefix



```

fun: noout_add_category Add Category (Category Name)

fun: noout_delete_product Delete Product (Product ID)

```



When a function follows the pattern `prefix_noout_name`, its return value will not be displayed in the UI.



## Comments and Formatting



- Lines starting with `#` at the beginning are treated as comments and ignored:



```

# This is a comment

screen: read_functions #4F709C Reading data

```



- Empty lines are ignored

- `#` in the middle of a line (after a value) is treated as a literal character, \*\*not\*\* a comment marker



## Processing Order



- `fun:` entries apply to the most recently defined `screen:`

- Screens appear in the UI in the order they are defined in the config file



## Complete Example



```

# Database Connection

url: localhost:5432

database: postgres

user: postgres

password: postgres



# Function screens

screen: read_functions #4F709C Reading data

fun: all_categories Show all categories

fun: products_by_category Show all products from the specified category (Id of the category)



screen: insert_functions #5F8D4E Inserting new data

fun: insert_product Add Product (Name of the product, Price, Id of the category)



screen: remove\_functions #A75D5D Removing data

```

