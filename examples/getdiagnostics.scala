expectIncoming(matching { case gd: GetDiagnosticsReq => })

send(DiagnosticsStatusNotificationReq(DiagnosticsStatus.Uploading))
expectIncoming(matching { case DiagnosticsStatusNotificationRes => })

send(DiagnosticsStatusNotificationReq(DiagnosticsStatus.Uploaded))
expectIncoming(matching { case DiagnosticsStatusNotificationRes => })
