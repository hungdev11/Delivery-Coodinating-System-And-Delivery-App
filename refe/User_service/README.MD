# User Module

## Run act locally
### Requirement
- [act](https://github.com/nektos/act)
- [docker](https://www.docker.com/)
- [gh CLI](https://cli.github.com/)
### Run
> Run for testing
```bash
gh act push -e ".actrc\event.json"
```
> Run for production
```bash
gh act push -s DEPLOY_TOKEN="$(gh auth token)"
```

> Explanation:
> [.actrc\event.json](.actrc\event.json) file will skip login and push to github package registry
> `DEPLOY_TOKEN` is a token for production
