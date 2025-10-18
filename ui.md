# Problem
- i wanna make a ui (continue , in fact) that manage my [system](/BE/api-gateway/.docs/route/README.md)
- i have base design like this: [FE](/ManagementSystem); this will manage by modules, each module will have its own route and its own ui, then export and use in [routing](/ManagementSystem/src/router/index.ts)

# Tech stack (all installed in the project)
- [Vue 3](https://vuejs.org/)
- [Vue Router](https://router.vuejs.org/)
- [Pinia](https://pinia.vuejs.org/)
- [Vue Use](https://vueuse.org/)
- [Nuxt UI](https://ui.nuxt.com/docs/getting-started), this have mcp server, so you can call and use
- [Tailwind CSS v4](https://tailwindcss.com/)
- [Track-asia-font](https://www.track-asia.com) - for map display, select point/waypoint, etc.

# Requirement
- Easy to use and maintain
- Split UI and logic, don't mix them together
- Must have documentation, so i can understand and use it
- Make a readable code
- Don't need unit test

# Advice
- Make a common component first (like table, form, etc.)
- Define style guide for the project (with a default theme)
- Make it able to build and deploy to production
