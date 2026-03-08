# Fix Edit Product Logic and Restore Multi-Image Upload

## Problem Analysis

The user reported that the "Edit" operation on the "My Products" page has no effect. Upon inspection of `ProductManage.vue`, it was found that the file content has reverted to a state where:

1. The multi-image upload feature (previously implemented) is missing.
2. The "Edit" button logic (`handleEditProduct`) is simplistic and does not handle image data parsing.
3. The "Upload Image" button in the dialog is a dummy button with no click handler.
4. The `el-upload` component is missing from the template.

## Goal

Restore the multi-image upload functionality and fix the "Edit" operation to correctly load and save product data, including multiple images.

## Implementation Steps

### 1. Update Data Structure in `ProductManage.vue`

* Add `imageList` to the `data()` return object to manage the file list for `el-upload`.

* Ensure `productForm` includes an `images` property initialized as an empty array.

### 2. Update Template (Dialog Section)

* Replace the current "Cover Image" form item (which has a readonly input and a dummy button) with the `el-upload` component.

* Configure `el-upload`:

  * `action`: Point to `/api/upload/product`

  * `list-type`: `picture-card`

  * `file-list`: Bind to `imageList`

  * `on-success`: Bind to `handleUploadSuccess`

  * `on-remove`: Bind to `handleRemove`

  * `before-upload`: Bind to `beforeUpload`

  * `limit`: Set a reasonable limit (e.g., 5 or 9 images)

### 3. Implement/Restore Methods

* **`handleEditProduct(product)`**:

  * Deep copy product data to `productForm`.

  * Parse `product.images` (JSON string) into `this.productForm.images` (Array).

  * Map `productForm.images` to `this.imageList` format (`{ name, url }`) for the uploader.

  * Set `isDialogVisible = true`.

* **`handleSaveProduct()`**:

  * Convert `productForm.images` array back to JSON string before sending to the API.

  * Ensure the first image in the list is set as the main `image` (cover).

* **`handleUploadSuccess(response, file, fileList)`**:

  * Update `imageList` and `productForm.images` with the new file URL.

  * Update the cover image (`productForm.image`) if it's the first image.

* **`handleRemove(file, fileList)`**:

  * Update `imageList` and `productForm.images`.

  * Update the cover image logic.

* **`beforeUpload(file)`**:

  * Validate file type (JPG/PNG) and size.

### 4. Verification

* Verify that clicking "Edit" opens the dialog.

* Verify that existing images are correctly displayed in the uploader.

* Verify that adding/removing images updates the state correctly.

* Verify that saving sends the correct JSON string for `images` to the backend.

