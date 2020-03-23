CREATE TABLE ofMultiplexer (
  username              VARCHAR(64)     NOT NULL,
  resource              VARCHAR(64)     NOT NULL,
  PRIMARY KEY (username, resource)
);

INSERT INTO ofVersion (name, version) VALUES ('multiplexer', 1);