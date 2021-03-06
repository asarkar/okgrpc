# OkGRPC

gRPC Java client and CLI based on [gRPC Server Reflection](https://github.com/grpc/grpc/blob/master/doc/server-reflection.md).
Can be used to inspect gRPC services and execute RPC methods dynamically without needing a proto file. The client can
be directly used from any JVM source code, while the CLI can be executed on command line. No installation necessary,
only needs Java 8 or later.

[![](https://github.com/asarkar/okgrpc/workflows/CI%20Pipeline/badge.svg)](https://github.com/asarkar/okgrpc/actions?query=workflow%3A%22CI+Pipeline%22)


## CLI Usage

You can find the latest version on Bintray. [ ![Download](https://api.bintray.com/packages/asarkar/mvn/com.asarkar.grpc%3Aokgrpc-cli/images/download.svg) ](https://bintray.com/asarkar/mvn/com.asarkar.grpc%3Aokgrpc-cli/_latestVersion)

It's also on jcenter.

It's an executable JAR. Run with `--help` for main CLI usage, and `<command> --help` for specific command usage.

> For brevity, I show `java -jar okgrpc-cli-<version>.jar` simply as `okgrpc-cli` below.

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
  > -h "key: value" \
  > com.asarkar.okgrpc.test.GreetingService.Greet \
  > '{ "greeting": { "name" : "test" } }'
{
  "result": "Hello, test"
}
```

It is perfectly fine to send multiple requests for client streaming calls; each string needs to be a valid JSON 
representing the Protobuf request object.

Binary data may be sent in the request as valid UTF-8 encoded string (like Base64). Of course, the server needs to 
know that and decode accordingly.

It is possible to use local `.proto` files if reflection isn't enabled on the server.
```
$ okgrpc-cli -a localhost:64575 exec \
  > --proto-path=<directory in which to search for proto file imports> \
  > --proto-file=<service proto file relative to the proto paths> \
  > com.asarkar.okgrpc.test.GreetingService.Greet \
  > '{ "greeting": { "name" : "test" } }'
{
  "result": "Hello, test"
}
```

## Client Usage

You can find the latest version on Bintray. [ ![Download](https://api.bintray.com/packages/asarkar/mvn/com.asarkar.grpc%3Aokgrpc-client/images/download.svg) ](https://bintray.com/asarkar/mvn/com.asarkar.grpc%3Aokgrpc-client/_latestVersion)

It's also on jcenter.

```kotlin
val client = OkGrpcClient.Builder()
    .withChannel(ManagedChannelFactory.getInstance(address))
    .build()
```

All the methods are defined as extension functions in the `OkGrpcClientExtn` file, that from Java, will become `static`
methods in `OkGrpcClientExtnKt` class.

See KDoc for more details.

## Contribute

This project is a volunteer effort. You are welcome to send pull requests, ask questions, or create issues.
If you like it, you can help by spreading the word!

## License

Copyright 2020 Abhijit Sarkar - Released under [Apache License v2.0](LICENSE).
