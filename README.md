sling
============

plant
==

Adds a new item to the list of items, returning the new item's object ID.

Parameters
--

* **seed**: the file to associate with the new item

* **latitude**: latitude of the new item

* **longitude**: the longitude of the new item

* **title**: title to associate with the new item

* **expiration**: number of seconds that the new item will expire in

* **password** (optional): the password as a string, in encrypted form (sha1 with salt `NSGbUvrdqwqHYFAQ`)

* **question** (optional, requires `password`): question as a string to display for verification. If
	provided, the answer is assumed to be `password`

get
==

Serves the file associated with a specific item.

Parameters
--

* **key**: the object ID of the item to fetch. Does not serve anything if this item has expired

* **password** (optional): user-supplied password or question guess for this item, unencrypted. Does not
serve anything if password is incorrect.

list
==

Generates a JSON-formatted list of metadata for all unexpired items. Each item in the list contains the
following fields:

* **expiration**: the expiration date of the item, in ISO format (YYYY-MM-DD)

* **created**: the date that the item was created, in ISO format (YYYY-MM-DD)

* **distance**: the distance between the coordinate at `(latitude, longitude)` and the current item, in km

* **key**: the object ID of the item

The resulting list is sorted by distance from the given coordinates, in ascending order.

Filter faraway items on the client's end.

Parameters
--

* **latitude**: user's current latitude

* **longitude**: user's current longitude

gc
==

Cleans up the list of items by deleting all expired items.
