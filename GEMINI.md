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
- Use your native editing commands and not shell hacks(like cat, sed,  running python or bash script, etc use your own read and edit/write method); if you are having problem with them then web search to see how to do it
- don't read gradle cache, read the files in the dir
- don't run git commands unless it is to read the file from older commit, I do not want you to commit, etc just read if anything else I'll do it
- Always add necessary import statements at the top of the file instead of using fully qualified class names inline.
