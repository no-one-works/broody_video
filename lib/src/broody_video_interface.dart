import 'dart:io';
import 'dart:typed_data';
import 'dart:ui';

import 'package:broody_video/broody_video.dart';
import 'package:loading_value/loading_value.dart';

abstract class BroodyVideoInterface {
  Stream<LoadingValue<MediaInfo?>> processClip({
    required File sourceFile,
    Duration start = Duration.zero,
    Duration? duration,
    Size? targetSize,
  });

  Stream<LoadingValue<MediaInfo?>> concatVideos({
    required List<String> sourcePaths,
    required File destination,
  });

  Future<Uint8List?> getThumbnail({
    required File sourceFile,
    Duration position = Duration.zero,
    int quality = 100,
  });

  Future<void> clearCache();
}
