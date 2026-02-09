import { api } from "./api";

export function getAllMessages() {
  return api.get("/api/messages").then((response) => response.data);
}

export function createMessage(message) {
  return api.post("/api/messages", message).then((response) => response.data);
}
