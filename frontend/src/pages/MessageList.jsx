import { useState, useEffect } from "react";
import { Typography, Button, Box, Link } from "@mui/material";
import { Link as RouterLink } from "react-router-dom";

import { getAllMessages } from "../services/message";
import { getAuthenticatedUser } from "../services/user";

export default function MessageList() {
  const [messages, setMessages] = useState();
  const [user, setUser] = useState();

  useEffect(() => {
    getAllMessages().then((messages) => setMessages(messages));
  }, []);

  useEffect(() => {
    getAuthenticatedUser().then((user) => setUser(user));
  }, []);

  return (
    <>
      <Typography variant="h4" component="h1" sx={{ marginBottom: 2 }}>
        Messages
      </Typography>
      {messages && (
        <ul>
          {messages.map((message) => (
            <li key={message.id}>
              {message.user?.username} {new Date(message.createdAt).toLocaleDateString('fi-FI')}: {message.content}
            </li>
          ))}
        </ul>
      )}

      <Box sx={{ marginTop: 2 }}>
        {user ? (
          <Button component={RouterLink} to="/messages/add" variant="contained">
            Add a message
          </Button>
        ) : (
          <Typography>
            <Link component={RouterLink} to="/register">
              Register
            </Link>{" "}
            or{" "}
            <Link component={RouterLink} to="/login">
              login
            </Link>{" "}
            to add messages.
          </Typography>
        )}
      </Box>
    </>
  );
}
