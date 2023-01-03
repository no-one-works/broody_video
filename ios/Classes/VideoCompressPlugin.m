#import "VideoCompressPlugin.h"
#import <broody_video/broody_video-Swift.h>

@implementation VideoCompressPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [BroodyVideoPlugin registerWithRegistrar:registrar];
}
@end
