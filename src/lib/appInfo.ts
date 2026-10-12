/** Single source of truth for the shipped version.
 *  Before this existed, three different places showed three different
 *  versions (v1.2.0 in About, v1.3.0 on the APK label, v1.2.0 on the button). */
export const APP_VERSION = '1.5.1';

/** Rough download size shown next to the APK button (the exact number is
 *  only known after CI builds it; this keeps the label honest without a
 *  request just to read Content-Length). */
export const APK_SIZE_LABEL = '~5 MB';
