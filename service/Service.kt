/*
 * Copyright (C) 2022 Vaticle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.vaticle.typedb.studio.service

import com.vaticle.typedb.studio.service.common.ConfirmationService
import com.vaticle.typedb.studio.service.common.DataService
import com.vaticle.typedb.studio.service.common.EditorService
import com.vaticle.typedb.studio.service.common.NotificationService
import com.vaticle.typedb.studio.service.common.PreferenceService
import com.vaticle.typedb.studio.service.common.StatusService
import com.vaticle.typedb.studio.service.connection.ClientState
import com.vaticle.typedb.studio.service.page.PageService
import com.vaticle.typedb.studio.service.project.ProjectService
import com.vaticle.typedb.studio.service.schema.SchemaService

object Service {

    lateinit var preference: PreferenceService
    lateinit var data: DataService
    lateinit var editor: EditorService
    lateinit var status: StatusService
    lateinit var notification: NotificationService
    lateinit var confirmation: ConfirmationService
    lateinit var pages: PageService
    lateinit var client: ClientState
    lateinit var project: ProjectService
    lateinit var schema: SchemaService

    init {
        init()
    }

    fun init() {
        data = DataService()
        preference = PreferenceService(data)
        editor = EditorService()
        status = StatusService()
        notification = NotificationService()
        confirmation = ConfirmationService()
        pages = PageService()
        client = ClientState(notification, preference)
        project = ProjectService(preference, data, notification, confirmation, client, pages)
        schema = SchemaService(client.session, pages, notification, confirmation)
    }
}
