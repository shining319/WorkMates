import { createBrowserRouter } from 'react-router'
import { AppLayout } from './layouts/AppLayout'
import { PublicLayout } from './layouts/PublicLayout'
import { PlaceholderPage } from './PlaceholderPage'
import { RequireRole } from './RequireRole'

// Only the routes needed for registration and roles exist so far. Each feature
// adds its own routes from architecture §9.5 and swaps out the placeholders.
export const router = createBrowserRouter([
  {
    element: <PublicLayout />,
    children: [
      { path: '/', element: <PlaceholderPage title="WorkMates" /> },
      { path: '/login', element: <PlaceholderPage title="Log in" /> },
      { path: '/register', element: <PlaceholderPage title="Register" /> },
    ],
  },
  {
    element: <RequireRole />,
    children: [
      {
        element: <AppLayout />,
        children: [
          {
            element: <RequireRole allowed="STUDENT" />,
            children: [
              { path: '/jobs', element: <PlaceholderPage title="Jobs" /> },
            ],
          },
          {
            element: <RequireRole allowed="EMPLOYER" />,
            children: [
              {
                path: '/postings',
                element: <PlaceholderPage title="My postings" />,
              },
            ],
          },
        ],
      },
    ],
  },
])
