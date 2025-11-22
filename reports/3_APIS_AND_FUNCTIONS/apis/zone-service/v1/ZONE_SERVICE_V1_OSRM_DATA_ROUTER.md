**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)

---

# Zone Service API: OSRM Data Router (v1)

**Base Path:** `/api/v1/osrm`

This router, part of the `zone-service`, handles the management of OSRM data and instances.

| Method   | Path                  | Business Function                               | Controller Method       |
|----------|-----------------------|-------------------------------------------------|-------------------------|
| `POST`   | `/build/{instanceId}` | Trigger a build for an OSRM instance.           | `buildInstance`         |
| `POST`   | `/build-all`          | Trigger a build for all OSRM instances.         | `buildAllInstances`     |
| `POST`   | `/start/{instanceId}` | Start an OSRM instance.                         | `startInstance`         |
| `POST`   | `/stop/{instanceId}`  | Stop an OSRM instance.                          | `stopInstance`          |
| `POST`   | `/rolling-restart`    | Perform a rolling restart of OSRM instances.    | `rollingRestart`        |
| `GET`    | `/status`             | Get the status of all OSRM instances.           | `getAllInstancesStatus` |
| `GET`    | `/status/{instanceId}`| Get the status of a specific OSRM instance.   | `getInstanceStatus`     |
| `GET`    | `/health`             | Health check for OSRM.                          | `healthCheck`           |
| `GET`    | `/validate/{instanceId}`| Validate the data for an OSRM instance.       | `validateData`          |
| `GET`    | `/history`            | Get the build history for all instances.        | `getAllBuildHistory`    |
| `GET`    | `/history/{instanceId}` | Get the build history for a specific instance.| `getBuildHistory`       |
| `GET`    | `/deployment`         | Get the deployment status.                      | `getDeploymentStatus`   |


---

**Navigation**: [ Back to API Documentation](api_documentation.md) | [ APIs and Functions](../README.md) | [ Report Index](../../README.md)