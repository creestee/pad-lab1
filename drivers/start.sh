#!/bin/sh

if [ "$SERVICE_PORT" -eq "0" ]; then
  # Assign a unique port for this replica
  # You can use a simple loop to find an available port in your desired range
  for port in {8080..8100}; do
    (echo > /dev/tcp/127.0.0.1/$port) &>/dev/null
    if [ $? -ne 0 ]; then
      export SERVICE_PORT="$port"
      break
    fi
  done
fi

# Start your service with the assigned port
exec your_command --port "$SERVICE_PORT"