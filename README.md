# benwiz/spotify-tools

How to run:

```
yarn install
yarn watch
```

https://developer.spotify.com/policy

## Netlify

For now it seems like Netlify does not have Java 11 so I am using `netlify build && netlify deploy -d resources/public/ --prod`. Drop `--prod` for demo. 

Alias under:

```yarn deploy```

I think eventually I can include a Java 11 binary for shadow-cljs to use which would enable Netlify to build when new code is pushed to main branch.

## To Do

- permanently show Currently Playing card... generalize the card from Analysis
- use generalized card in Power Hour
- cache downloaded data... maybe re-frame offers a way to do this with the db or maybe just have to write to LocalStorage directly

