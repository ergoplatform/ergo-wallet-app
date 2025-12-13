# Error Message Display Fix

## Problem
API error responses with custom error messages (e.g., "Address already attached to an account") are not being displayed. Instead, only the HTTP status text (e.g., "Conflict") is shown to users.

## Root Cause
The `getErrorMessage` function in `frontend/src/lib/utils.ts` was joining error and message fields with ` - ` and falling back to Axios error code/message, which prioritizes HTTP status descriptions over custom API error messages.

## Solution

Replace the current `getErrorMessage` function with this improved version:

```typescript
export function getErrorMessage(error: unknown): string {
  if (!error) return 'Something went wrong';

  // Check if error is an AxiosError
  if ((error as AxiosError).isAxiosError) {
    const axiosErr = error as AxiosError<any>;
    const resData = axiosErr.response?.data;

    // Priority 1: Check for error field in response
    if (resData?.error) {
      return resData.error;
    }

    // Priority 2: Check for message field in response
    if (resData?.message) {
      return resData.message;
    }

    // Priority 3: Use status text from response
    if (axiosErr.response?.statusText) {
      return axiosErr.response.statusText;
    }

    // Priority 4: Fallback to Axios error message
    return axiosErr.message || 'Something went wrong';
  }

  // Fallback for non-Axios errors
  return (error as Error).message || 'Something went wrong';
}
```

## Changes Made

1. **Prioritize custom error field**: Check `resData.error` first
2. **Fallback to message field**: Use `resData.message` if error not present
3. **Remove error code prefix**: No longer shows "ERROR:" or status codes
4. **Remove join logic**: Don't combine error and message fields
5. **Better fallback chain**: Only use generic messages when no custom message exists

## Testing

### Before Fix:
```
API Response: { error: "Address already attached to an account.", message: "..." }
Displayed: "Conflict" (HTTP 409 status text)
```

### After Fix:
```
API Response: { error: "Address already attached to an account.", message: "..." }
Displayed: "Address already attached to an account."
```

## File Location
`frontend/src/lib/utils.ts` (line ~25786)

## Benefits

✅ **User-friendly**: Shows meaningful error messages from the API  
✅ **Consistent**: Always prioritizes custom errors over generic HTTP statuses  
✅ **Maintainable**: Clear priority chain for error message selection  
✅ **Backward compatible**: Still handles cases where no custom error exists
