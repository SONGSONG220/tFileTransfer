#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface Address : NSObject

+ (NSArray<NSString *> *)findLocalIPv4Addresses;

+ (nullable NSString *)broadcastAddressFor:(NSString *)ip;

@end

NS_ASSUME_NONNULL_END
