# OkGRPC

gRPC Java client and CLI based on [gRPC Server Reflection](https://github.com/grpc/grpc/blob/master/doc/server-reflection.md).
Can be used to inspect gRPC services and execute gRPC methods dynamically without needing a proto file. The client can
be directly used from any JVM source code, while the CLI can be executed on command line. No installation necessary,
only needs Java 8 or later.

[![](https://github.com/asarkar/okgrpc/workflows/CI%20Pipeline/badge.svg)](https://github.com/asarkar/okgrpc/actions?query=workflow%3A%22CI+Pipeline%22)

## Installation

You can find the latest version on Bintray. [ ![Download](https://api.bintray.com/packages/asarkar/mvn/com.asarkar.grpc%3Aokgrpc/images/download.svg) ](https://bintray.com/asarkar/mvn/com.asarkar.grpc%3Aokgrpc/_latestVersion)

## CLI Usage

Get all services:
```
$ okgrpc-cli -a localhost:64575 get
com.asarkar.okgrpc.test.GreetingService
grpc.reflection.v1alpha.ServerReflection
```

Describe a service:
```
$ okgrpc-cli -a localhost:64575 desc -s com.asarkar.okgrpc.test.GreetingService
name: "com/asarkar/okgrpc/test/greeting_service.proto"
package: "com.asarkar.okgrpc.test"
dependency: "com/asarkar/okgrpc/test/greeting.proto"
service {
  name: "GreetingService"
  method {
    name: "Greet"
    input_type: ".com.asarkar.okgrpc.test.GreetRequest"
    output_type: ".com.asarkar.okgrpc.test.GreetResponse"
    options {
    }
  }
  // more methods elided
}
```
Describe a method:
```
$ okgrpc-cli -a localhost:64575 desc -m com.asarkar.okgrpc.test.GreetingService.Greet
name: "Greet"
input_type: ".com.asarkar.okgrpc.test.GreetRequest"
output_type: ".com.asarkar.okgrpc.test.GreetResponse"
options {
}
```

Describe a type:
```
$ okgrpc-cli -a localhost:64575 desc -t com.asarkar.okgrpc.test.GreetRequest
name: "GreetRequest"
field {
  name: "greeting"
  number: 1
  label: LABEL_OPTIONAL
  type: TYPE_MESSAGE
  type_name: ".com.asarkar.okgrpc.test.Greeting"
}
```

Execute a method:
```
$ okgrpc-cli -a localhost:64575 exec \
  > com.asarkar.okgrpc.test.GreetingService.Greet \
  > '{ "greeting": { "name" : "test" } }'
{
  "result": "Hello, test"
}
```

## Client Usage

```
OkGrpcClient.Builder()
    .withChannel(ManagedChannelFactory.getInstance(address))
    .build()
```

See KDoc for more details.

## Contribute

This project is a volunteer effort. You are welcome to send pull requests, ask questions, or create issues.
If you like it, you can help by spreading the word!

## License

Copyright 2020 Abhijit Sarkar - Released under [Apache License v2.0](LICENSE).
