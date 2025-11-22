**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)

---

# API Documentation: QR Controller (v1)

**Base Path:** `/api/v1/qr`

This controller handles QR code generation by proxying requests to the `session-service`.

| Method | Path        | Business Function                        | Java Method Name | Proxied To        |
|--------|-------------|------------------------------------------|------------------|-------------------|
| `GET`  | `/generate` | Generate a QR code from the given data. | `generateQR`     | `session-service` |


---

**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)