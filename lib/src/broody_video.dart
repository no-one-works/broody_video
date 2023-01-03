import 'dart:async';
import 'dart:io';
import 'dart:typed_data';
import 'dart:ui';

import 'package:broody_video/src/broody_video_interface.dart';
import 'package:broody_video/src/media/media_info.dart';
import 'package:flutter/services.dart';
import 'package:loading_value/loading_value.dart';

class BroodyVideo implements BroodyVideoInterface {
  BroodyVideo._() {
    _channel.setMethodCallHandler(_methodCallHandler);
  }

  static BroodyVideo? _instance;

  static BroodyVideo get instance {
    if (_instance == null) {
      _instance = BroodyVideo._();
    }
    return _instance!;
  }

  final MethodChannel _channel = MethodChannel("broody_video");

  StreamController<LoadingValue<MediaInfo?>>? _currentProgress$;

  @override
  Stream<LoadingValue<MediaInfo?>> processClip({
    required File sourceFile,
    Duration start = Duration.zero,
    Duration? duration,
    Size? targetSize,
  }) async* {
    if (!(_currentProgress$?.isClosed ?? true)) {
      throw StateError("Only one clip can be processed at a time!");
    }

    _currentProgress$ = StreamController<LoadingValue<MediaInfo?>>();
    final progress$ = _currentProgress$!;

    final Future<Map?> process = _channel.invokeMethod<Map>(
      "processClip",
      {
        "sourcePath": sourceFile.path,
        "startSeconds": start.inMilliseconds / 1000,
        "durationSeconds":
            duration == null ? null : duration.inMilliseconds / 1000,
        "targetWidth": targetSize?.width.toInt(),
        "targetHeight": targetSize?.height.toInt(),
      },
    ).then((r) {
      progress$.close();
      return r;
    }).catchError((error, stackTrace) {
      _currentProgress$?.add(LoadingValue.error(error, stackTrace: stackTrace));
      _currentProgress$?.close();
    });

    await for (final value in progress$.stream) {
      yield value;
    }
    final result = await process;
    if (result != null) {
      try {
        yield LoadingValue.data(
          MediaInfo.fromJson(
            Map<String, dynamic>.from(result),
          ),
        );
      } catch (e, s) {
        yield LoadingValue.error(e, stackTrace: s);
      }
    } else {
      yield LoadingValue.data(null);
    }
  }

  @override
  Stream<LoadingValue<MediaInfo?>> concatVideos({
    required List<String> sourcePaths,
    required File destination,
  }) async* {
    if (!(_currentProgress$?.isClosed ?? true)) {
      throw StateError("Only one clip can be processed at a time!");
    }

    _currentProgress$ = StreamController<LoadingValue<MediaInfo?>>();
    final progress$ = _currentProgress$!;

    final Future<Map?> process = _channel.invokeMethod<Map>(
      "concatVideos",
      {
        "sourcePaths": sourcePaths,
        "destinationPath": destination.path,
      },
    ).then((r) {
      progress$.close();
      return r;
    }).catchError((error, stackTrace) {
      _currentProgress$?.add(LoadingValue.error(error, stackTrace: stackTrace));
      _currentProgress$?.close();
    });

    await for (final value in progress$.stream) {
      yield value;
    }
    final result = await process;
    if (result != null) {
      try {
        yield LoadingValue.data(
          MediaInfo.fromJson(
            Map<String, dynamic>.from(result),
          ),
        );
      } catch (e, s) {
        yield LoadingValue.error(e, stackTrace: s);
      }
    } else {
      yield LoadingValue.data(null);
    }
  }

  @override
  Future<Uint8List?> getThumbnail({
    required File sourceFile,
    Duration position = Duration.zero,
    int quality = 100,
  }) async {
    return await _channel.invokeMethod(
      "getThumbnail",
      {
        "sourcePath": sourceFile.path,
        "positionSeconds": position.inMilliseconds / 1000,
        "quality": quality,
      },
    );
  }

  Future<void> clearCache() async {
    await _channel.invokeMethod("clearCache");
  }

  Future<void> _methodCallHandler(MethodCall call) async {
    final args = double.tryParse(call.arguments);
    if (call.method == "updateProgress" && args != null) {
      _currentProgress$?.add(LoadingValue.loading(args));
    }
  }
}
