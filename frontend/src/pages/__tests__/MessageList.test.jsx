import { describe, it, expect, beforeAll, afterEach, afterAll } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import MessageList from "../MessageList";

const API_BASE_URL = import.meta.env.VITE_API_URL;

const server = setupServer();

beforeAll(() => server.listen({ onUnhandledRequest: "warn" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

function renderWithRouter(component) {
  return render(<BrowserRouter>{component}</BrowserRouter>);
}

describe("MessageList", () => {
  it("renders the Messages heading", () => {
    server.use(
      http.get(`${API_BASE_URL}/api/messages`, () => {
        return HttpResponse.json([]);
      }),
      http.get(`${API_BASE_URL}/api/users/current`, () => {
        return HttpResponse.json(null, { status: 401 });
      })
    );

    renderWithRouter(<MessageList />);
    expect(screen.getByRole("heading", { name: /messages/i })).toBeInTheDocument();
  });

  it("displays messages from API", async () => {
    const mockMessages = [
      { id: 1, content: "Hello World", createdAt: "2024-10-15T10:30:00.000Z", user: { username: "user1" } },
      { id: 2, content: "Test Message", createdAt: "2024-11-16T12:45:00.000Z", user: { username: "user2" } },
    ];

    server.use(
      http.get(`${API_BASE_URL}/api/messages`, () => {
        return HttpResponse.json(mockMessages);
      }),
      http.get(`${API_BASE_URL}/api/users/current`, () => {
        return HttpResponse.json(null, { status: 401 });
      })
    );

    renderWithRouter(<MessageList />);

    await waitFor(() => {
      expect(screen.getByText("user1 15.10.2024: Hello World")).toBeInTheDocument();
      expect(screen.getByText("user2 16.11.2024: Test Message")).toBeInTheDocument();
    });
  });

  it("displays message content when user is null", async () => {
    const mockMessages = [
      { id: 1, content: "Anonymous message", createdAt: "2024-12-17T08:15:00.000Z", user: null },
    ];

    server.use(
      http.get(`${API_BASE_URL}/api/messages`, () => {
        return HttpResponse.json(mockMessages);
      }),
      http.get(`${API_BASE_URL}/api/users/current`, () => {
        return HttpResponse.json(null, { status: 401 });
      })
    );

    renderWithRouter(<MessageList />);

    await waitFor(() => {
      expect(screen.getByText(/17\.12\.2024: Anonymous message/i)).toBeInTheDocument();
    });
  });

  it("shows 'Add a message' button when user is authenticated", async () => {
    const mockUser = { id: 1, username: "testuser" };

    server.use(
      http.get(`${API_BASE_URL}/api/messages`, () => {
        return HttpResponse.json([]);
      }),
      http.get(`${API_BASE_URL}/api/users/current`, () => {
        return HttpResponse.json(mockUser);
      })
    );

    renderWithRouter(<MessageList />);

    await waitFor(() => {
      expect(screen.getByRole("link", { name: /add a message/i })).toBeInTheDocument();
    });
  });

  it("shows login and register links when user is not authenticated", async () => {
    server.use(
      http.get(`${API_BASE_URL}/api/messages`, () => {
        return HttpResponse.json([]);
      }),
      http.get(`${API_BASE_URL}/api/users/current`, () => {
        return HttpResponse.json(null, { status: 401 });
      })
    );

    renderWithRouter(<MessageList />);

    await waitFor(() => {
      expect(screen.getByRole("link", { name: /register/i })).toBeInTheDocument();
      expect(screen.getByRole("link", { name: /login/i })).toBeInTheDocument();
      expect(screen.getByText(/to add messages/i)).toBeInTheDocument();
    });
  });

  it("handles empty message list", async () => {
    server.use(
      http.get(`${API_BASE_URL}/api/messages`, () => {
        return HttpResponse.json([]);
      }),
      http.get(`${API_BASE_URL}/api/users/current`, () => {
        return HttpResponse.json(null, { status: 401 });
      })
    );

    renderWithRouter(<MessageList />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: /messages/i })).toBeInTheDocument();
    });

    const listItems = screen.queryAllByRole("listitem");
    expect(listItems).toHaveLength(0);
  });

  it("renders multiple messages correctly", async () => {
    const mockMessages = [
      { id: 1, content: "First message", createdAt: "2024-09-20T09:00:00.000Z", user: { username: "alice" } },
      { id: 2, content: "Second message", createdAt: "2024-10-21T10:00:00.000Z", user: { username: "bob" } },
      { id: 3, content: "Third message", createdAt: "2024-11-22T11:00:00.000Z", user: { username: "charlie" } },
    ];

    server.use(
      http.get(`${API_BASE_URL}/api/messages`, () => {
        return HttpResponse.json(mockMessages);
      }),
      http.get(`${API_BASE_URL}/api/users/current`, () => {
        return HttpResponse.json(null, { status: 401 });
      })
    );

    renderWithRouter(<MessageList />);

    await waitFor(() => {
      const listItems = screen.getAllByRole("listitem");
      expect(listItems).toHaveLength(3);
      expect(screen.getByText("alice 20.9.2024: First message")).toBeInTheDocument();
      expect(screen.getByText("bob 21.10.2024: Second message")).toBeInTheDocument();
      expect(screen.getByText("charlie 22.11.2024: Third message")).toBeInTheDocument();
    });
  });
});
