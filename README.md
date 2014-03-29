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

Generates a JSON-formatted list of metadata for all unexpired items within the given radius in m.
Each item in the list contains the
following fields:

* **filename**: the original filename of the uploaded file

* **content_type**: the MIME/Media type of the original file

* **latitude**: the latitude of the item

* **longitude**: the longitude of the item

* **title**: the user-assigned title associated with the item

* **expiration**: the expiration date of the item, in ISO format (YYYY-MM-DDTHH:MM:SS.mmmmmm)

* **created**: the date that the item was created, in ISO format (YYYY-MM-DDTHH:MM:SS.mmmmmm)

* **distance**: the distance between the coordinate at `(latitude, longitude)` and the current item, in m

* **key**: the object ID of the item

The resulting list is sorted by distance from the given coordinates, in ascending order.

Parameters
--

* **latitude**: user's current latitude

* **longitude**: user's current longitude

* **radius** (optional): radius (m) to search for items in. All items outside the given radius are not returned.
Set to infinity by default.

gc
==

Cleans up the list of items by deleting all expired items.
