import { useEffect, useState } from 'react'
import { EVENTS_ROUTE, parseRoute, routeHash, type Route } from './route'

function readRoute(): Route | null {
  return parseRoute(window.location.hash)
}

/**
 * The current route, kept in sync with the address bar. An address that no screen has goes to the events list, and
 * the address is replaced (not pushed), so the back button does not come back to the broken address.
 */
export function useRoute(): Route {
  const [route, setRoute] = useState<Route | null>(readRoute)

  useEffect(() => {
    const update = () => setRoute(readRoute())
    window.addEventListener('hashchange', update)
    return () => window.removeEventListener('hashchange', update)
  }, [])

  useEffect(() => {
    if (route === null) {
      window.location.replace(routeHash(EVENTS_ROUTE))
    }
  }, [route])

  return route ?? EVENTS_ROUTE
}
