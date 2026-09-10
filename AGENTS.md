- you will make the component adaptive so it looks(/scale) identical on different devices; activate the adaptive skill if working on ui
- don't take shortcuts while coding and try to shoe horn the solution
- if your assumptions about the solution is significant different (>65% for eg) don't try to fill in the gaps, stop and ask the user first, unless even they don't know anything
- you will use the latest docs and not outdated apis
- use Modern app architecture that consist of 
  1) Adaptive and layered architecture
  2) Unidirectional data flow (UDF) in all layers of the app
  3) UI layer with state holders to manage the complexity of the UI
  4) Coroutines and flows
  5) Dependency injection best practices 
- don't go exploring the files that are not needed, try to stick to files mentioned or implicitly mentioned, exploring other files that you know we don't need wasted token and bloat context
