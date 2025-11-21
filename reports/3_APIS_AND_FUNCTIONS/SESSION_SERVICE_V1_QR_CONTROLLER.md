# Session Service API: QR Controller (v1)

**Base Path:** `/api/v1/qr`

This controller handles QR code generation and decoding within the `session-service`.

| Method | Path        | Business Function                                                 | Java Method Name | Notes                                                                    |
|--------|-------------|-------------------------------------------------------------------|------------------|--------------------------------------------------------------------------|
| `GET`  | `/generate` | Generate a QR code from the given data and return it as a PNG image. | `generate`       | This is the backend implementation for the corresponding endpoint in the API gateway. |
| `POST` | `/decode`   | Decode a QR code from an uploaded image file.                     | `decode`         | This endpoint is not exposed through the API gateway.                    |
