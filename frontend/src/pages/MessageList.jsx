import { useState, useEffect } from "react";
import { Typography, Button, Box, Link } from "@mui/material";
import { Link as RouterLink } from "react-router-dom";

import { getAllMessages } from "../services/message";
import { getAuthenticatedUser } from "../services/user";

function getCreationTimeLabel(message) {
  const ageInMilliseconds = new Date() - new Date(message.createdAt);
  const ageInSeconds = ageInMilliseconds / 1000;
  const oneHourInSeconds = 60 * 60;
  const oneDayInSeconds = oneHourInSeconds * 24;

  if (ageInSeconds < 60) {
    return `Added just now`;
  } else if (ageInSeconds < oneHourInSeconds) {
    return `Added ${Math.floor(ageInSeconds / 60)} minutes ago`;
  } else if (ageInSeconds < oneDayInSeconds) {
    return `Added ${Math.floor(ageInSeconds / oneHourInSeconds)} hours ago`;
  } else {
    return `Added on ${new Date().toLocaleDateString("fi")}`;
  }
}

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
              {message.user?.username}: {message.content} ·{" "}
              {getCreationTimeLabel(message)}
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
